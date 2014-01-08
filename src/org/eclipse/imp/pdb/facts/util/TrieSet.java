/*******************************************************************************
 * Copyright (c) 2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("rawtypes")
public abstract class TrieSet<K> extends AbstractImmutableSet<K> {

	protected static final int BIT_PARTITION_SIZE = 5;
	protected static final int BIT_PARTITION_MASK = 0x1f;

	protected static final TrieSet EMPTY = new InplaceIndexNode(0, 0, new TrieSet[0], 0);

	@SafeVarargs
	public static final <K> ImmutableSet<K> of(K... elements) {
		@SuppressWarnings("unchecked")
		ImmutableSet<K> result = TrieSet.EMPTY;
		for (K k : elements) result = result.__insert(k);
		return result;
	}
	
	@SafeVarargs
	public static final <K> TransientSet<K> transientOf(K... elements) {
		TransientSet<K> transientSet = new TransientTrieSet<>();
		for (K k : elements) 
			transientSet.__insert(k);
		return transientSet;
	}
		
//	@SuppressWarnings("unchecked")
//	protected static final <K> Comparator<K> equalityComparator() {
//		return EqualityUtils.getDefaultEqualityComparator();
//	}
	
	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equivalenceComparator() {
		return EqualityUtils.getEquivalenceComparator();
	}
	
	@SuppressWarnings("unchecked")
	static <K> TrieSet<K> mergeNodes(Object node0, int hash0, Object node1, int hash1, int shift) {
		assert (!(node0 instanceof TrieSet));
		assert (!(node1 instanceof TrieSet));

		if (hash0 == hash1)
			return new HashCollisionNode<>(hash0, (K[]) new Object[]{node0, node1});

		final int mask0 = (hash0 >>> shift) & BIT_PARTITION_MASK;
		final int mask1 = (hash1 >>> shift) & BIT_PARTITION_MASK;

		if (mask0 != mask1) {
			// both nodes fit on same level
			final int bitmap = (1 << mask0) | (1 << mask1);
			final int valmap = (1 << mask0) | (1 << mask1);
			final Object[] nodes = new Object[2];

			if (mask0 < mask1) {
				nodes[0] = node0;
				nodes[1] = node1;
			} else {
				nodes[0] = node1;
				nodes[1] = node0;
			}

			return new InplaceIndexNode<>(bitmap, valmap, nodes, 2);
		} else {
			// values fit on next level
			final int bitmap = (1 << mask0);
			final int valmap = 0;
			final TrieSet node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

			return new InplaceIndexNode<>(bitmap, valmap, node, 1);
		}
	}

	static <K> TrieSet<K> mergeNodes(TrieSet node0, int hash0, TrieSet node1, int hash1, int shift) {
		final int mask0 = (hash0 >>> shift) & BIT_PARTITION_MASK;
		final int mask1 = (hash1 >>> shift) & BIT_PARTITION_MASK;

		if (mask0 != mask1) {
			// both nodes fit on same level
			final int bitmap = (1 << mask0) | (1 << mask1);
			final int valmap = 0;
			final Object[] nodes = new Object[2];

			if (mask0 < mask1) {
				nodes[0] = node0;
				nodes[1] = node1;
			} else {
				nodes[0] = node1;
				nodes[1] = node0;
			}

			return new InplaceIndexNode<>(bitmap, valmap, nodes, node0.size() + node1.size());
		} else {
			// values fit on next level
			final int bitmap = (1 << mask0);
			final int valmap = 0;
			final TrieSet node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

			return new InplaceIndexNode<>(bitmap, valmap, node, node0.size());
		}
	}

	static <K> TrieSet<K> mergeNodes(TrieSet node0, int hash0, Object node1, int hash1, int shift) {
		assert (!(node1 instanceof TrieSet));

		final int mask0 = (hash0 >>> shift) & BIT_PARTITION_MASK;
		final int mask1 = (hash1 >>> shift) & BIT_PARTITION_MASK;

		if (mask0 != mask1) {
			// both nodes fit on same level
			final int bitmap = (1 << mask0) | (1 << mask1);
			final int valmap = (1 << mask1);
			final Object[] nodes = new Object[2];

//			if (mask0 < mask1) {
//				nodes[0] = node0;
//				nodes[1] = node1;
//			} else {
				// inline node first
				nodes[0] = node1;
				nodes[1] = node0;
//			}

			return new InplaceIndexNode<>(bitmap, valmap, nodes, node0.size() + 1);
		} else {
			// values fit on next level
			final int bitmap = (1 << mask0);
			final int valmap = 0;
			final TrieSet node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

			return new InplaceIndexNode<>(bitmap, valmap, node, node.size());
		}
	}

	abstract TrieSet<K> updated(K key, int hash, int shift, Comparator<Object> cmp);
	
	abstract TrieSet<K> updated(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> cmp);

	@Override
	public TrieSet<K> __insert(K k) {
		return updated(k, k.hashCode(), 0, equivalenceComparator());
	}

	@Override
	public TrieSet<K> __insertEquivalent(K k, Comparator<Object> cmp) {
		return updated(k, k.hashCode(), 0, cmp);
	}
	
	/*
	 * TODO: support fast batch operations.
	 */
	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		@SuppressWarnings("unchecked")
		TrieSet<K> result = (TrieSet<K>) TrieSet.of();		
		for (K e : set)
			result = result.updated(e, e.hashCode(), 0, equivalenceComparator());		
		return result;
	}	

	/*
	 * TODO: support fast batch operations.
	 */
	@Override
	public ImmutableSet<K> __insertAllEquivalent(Set<? extends K> set, Comparator<Object> cmp) {
		@SuppressWarnings("unchecked")
		TrieSet<K> result = (TrieSet<K>) TrieSet.of();		
		for (K e : set)
			result = result.updated(e, e.hashCode(), 0, cmp);		
		return result;
	}
	
	abstract TrieSet<K> removed(K key, int hash, int shift, Comparator<Object> comparator);
	
	abstract TrieSet<K> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator);

	@Override
	public TrieSet<K> __remove(K k) {
		return removed(k, k.hashCode(), 0, equivalenceComparator());
	}

	@Override
	public TrieSet<K> __removeEquivalent(K k, Comparator<Object> cmp) {
		return removed(k, k.hashCode(), 0, cmp);
	}
	
	abstract boolean contains(Object key, int hash, int shift, Comparator<Object> comparator);

	@Override
	public boolean contains(Object o) {
		return contains(o, o.hashCode(), 0, equivalenceComparator());
	}
	
	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		return contains(o, o.hashCode(), 0, cmp);
	}

	abstract boolean hasValues();
	abstract Iterator<K> valueIterator();
	
	abstract boolean hasNodes();
	abstract Iterator<TrieSet<K>> nodeIterator();
}

@SuppressWarnings("rawtypes")
/*package*/ class InplaceIndexNode<K> extends TrieSet<K> {

	private AtomicReference<Thread> mutator;

	private int bitmap;
	private int valmap;
	private Object[] nodes;
	private int cachedSize;
	private int cachedValmapBitCount;
		
	InplaceIndexNode(AtomicReference<Thread> mutator, int bitmap, int valmap, Object[] nodes, int cachedSize) {
		assert (Integer.bitCount(bitmap) == nodes.length);

		this.mutator = mutator;
		
		this.bitmap = bitmap;
		this.valmap = valmap;
		this.nodes = nodes;

		this.cachedSize = cachedSize;
		this.cachedValmapBitCount = Integer.bitCount(valmap);
	}
		
	InplaceIndexNode(AtomicReference<Thread> mutator, Object[] nodes) {
		this.mutator = mutator;
		this.nodes = nodes;
	}
	
	InplaceIndexNode(int bitmap, int valmap, Object[] nodes, int cachedSize) {
		this(null, bitmap, valmap, nodes, cachedSize);
	}

	InplaceIndexNode(int bitmap, int valmap, Object node, int cachedSize) {
		this(bitmap, valmap, new Object[]{node}, cachedSize);
	}

	final int index(int bitpos) {
		return Integer.bitCount(bitmap & (bitpos - 1));
	}

	final int bitIndex(int bitpos) {
		return Integer.bitCount(valmap) + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
	}
	
	final int valIndex(int bitpos) {
		return Integer.bitCount(valmap & (bitpos - 1));
	}
	
	private void updateBitmaps(int bitmap, int valmap, int cachedValmapBitCount) {
		this.bitmap = bitmap;
		this.valmap = valmap;
		this.cachedValmapBitCount = cachedValmapBitCount; 
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(Object key, int hash, int shift, Comparator<Object> comparator) {
		final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
		final int bitpos = (1 << mask);

		if ((bitmap & bitpos) != 0) {			
			if ((valmap & bitpos) != 0) {
				return comparator.compare(nodes[valIndex(bitpos)], key) == 0;
			} else {
				return ((TrieSet<K>) nodes[bitIndex(bitpos)]).contains(key, hash, shift + BIT_PARTITION_SIZE, comparator);
			}
		}
		return false;
	}

	@Override
	public TrieSet<K> updated(K key, int hash, int shift, Comparator<Object> comparator) {
		final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
		final int bitpos = (1 << mask);
		final int bitIndex = bitIndex(bitpos);
		final int valIndex = valIndex(bitpos);
//		final int valmapBitCount = Integer.bitCount(valmap);

		if ((bitmap & bitpos) == 0) {
			// no entry, create new node with inplace value		
			final Object[] nodesReplacement = ArrayUtils.arraycopyAndInsert(nodes, valIndex, key);
			
			// immutable copy
			return new InplaceIndexNode<>(bitmap | bitpos, valmap | bitpos, nodesReplacement, this.size() + 1);	
		}

		if ((valmap & bitpos) != 0) {
			// it's an inplace value
			if (comparator.compare(nodes[valIndex], key) == 0)
				return this;

			final TrieSet nodeNew = mergeNodes(nodes[valIndex], nodes[valIndex].hashCode(), key, hash, shift + BIT_PARTITION_SIZE);
			
			// immutable copy
			/** CODE DUPLCIATION **/
			final int bitIndexNewOffset = Integer.bitCount(valmap & ~bitpos);
			final int bitIndexNew = bitIndexNewOffset + Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos)) & (bitpos - 1)); 
			final Object[] nodesReplacement = ArrayUtils.arraycopyAndMoveToBack(nodes, valIndex, bitIndexNew, nodeNew);
			
			return new InplaceIndexNode<>(bitmap | bitpos, valmap & ~bitpos, nodesReplacement, this.size() + 1);				
		} else {
			// it's a TrieSet node, not a inplace value
			@SuppressWarnings("unchecked")
			final TrieSet<K> subNode = (TrieSet<K>) nodes[bitIndex];

			// immutable copy subNode
			final TrieSet<K> subNodeReplacement = subNode.updated(key, hash, shift + BIT_PARTITION_SIZE, comparator);

			if (subNode == subNodeReplacement)
				return this;

			assert(subNode.size() == subNodeReplacement.size() - 1);

			final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, bitIndex, subNodeReplacement);
			return new InplaceIndexNode<>(bitmap, valmap, nodesReplacement, this.size() + 1);			
		}
	}
	
	InplaceIndexNode<K> editAndInsert(AtomicReference<Thread> mutator, int index, Object elementNew) {		
		Object[] editableNodes = ArrayUtils.arraycopyAndInsert(this.nodes, index, elementNew);
		
		if (this.mutator == mutator) {
			this.nodes = editableNodes;
			return this;
		} else {
			return new InplaceIndexNode<>(mutator, editableNodes);
		}
	}
	
	// TODO: only copy when not yet editable
	InplaceIndexNode<K> editAndMoveToBack(AtomicReference<Thread> mutator, int indexOld, int indexNew, Object elementNew) {
		Object[] editableNodes = ArrayUtils.arraycopyAndMoveToBack(this.nodes, indexOld, indexNew, elementNew);
		
		if (this.mutator == mutator) {
			this.nodes = editableNodes;
			return this;
		} else {
			return new InplaceIndexNode<>(mutator, editableNodes);
		}		
	}
	
	// TODO: only copy when not yet editable
	InplaceIndexNode<K> editAndSet(AtomicReference<Thread> mutator, int index, Object elementNew) {
		Object[] editableNodes = ArrayUtils.arraycopyAndSet(this.nodes, index, elementNew);
		
		if (this.mutator == mutator) {
			this.nodes = editableNodes;
			return this;
		} else {
			return new InplaceIndexNode<>(mutator, editableNodes);
		}
	}
	
	InplaceIndexNode<K> editAndRemove(AtomicReference<Thread> mutator, int index) {
		Object[] editableNodes = ArrayUtils.arraycopyAndRemove(this.nodes, index);
		
		if (this.mutator == mutator) {
			this.nodes = editableNodes;
			return this;
		} else {
			return new InplaceIndexNode<>(mutator, editableNodes);
		}
	}
	
	// TODO: ensure editability
	@Override
	public TrieSet<K> updated(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator) {
		final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
		final int bitpos = (1 << mask);
		final int bitIndex = bitIndex(bitpos);
		final int valIndex = valIndex(bitpos);

		if ((bitmap & bitpos) == 0) { // no value
			InplaceIndexNode<K> editableNode = editAndInsert(mutator, valIndex, key);
			editableNode.updateBitmaps(bitmap |= bitpos, valmap |= bitpos,
					cachedValmapBitCount + 1);			
			return editableNode;
		}

		if ((valmap & bitpos) != 0) { // inplace value
			if (comparator.compare(nodes[valIndex], key) == 0) {
				return this;
			} else {					
				// TODO: simplify this line
				int bitIndexNew = (cachedValmapBitCount - 1) + Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos)) & (bitpos - 1)); 								
				TrieSet nodeNew = mergeNodes(nodes[valIndex], nodes[valIndex].hashCode(), key, hash, shift + BIT_PARTITION_SIZE);				
				
				InplaceIndexNode<K> editableNode = editAndMoveToBack(mutator, valIndex, bitIndexNew, nodeNew);
				editableNode.updateBitmaps(bitmap |= bitpos, valmap &= ~bitpos,
						cachedValmapBitCount - 1);								
				return editableNode; 
			}
		} else { // node (not value)
			@SuppressWarnings("unchecked")
			TrieSet<K> oldNode = (TrieSet<K>) nodes[bitIndex];
			TrieSet<K> newNode = oldNode.updated(mutator, key, hash, shift + BIT_PARTITION_SIZE, comparator);
									
			return (newNode == oldNode) ? oldNode : editAndSet(mutator, valIndex, newNode);
		}
	}

	@Override
	public TrieSet<K> removed(K key, int hash, int shift, Comparator<Object> comparator) {
		final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
		final int bitpos = (1 << mask);
		final int bitIndex = bitIndex(bitpos);
		final int valIndex = valIndex(bitpos);

		if ((bitmap & bitpos) == 0)
			return this;

		if ((valmap & bitpos) == 0) {
			// it's a TrieSet node, not a inplace value
			@SuppressWarnings("unchecked")
			final TrieSet<K> subNode = (TrieSet<K>) nodes[bitIndex];
			
			final TrieSet<K> subNodeReplacement = subNode.removed(key, hash, shift + BIT_PARTITION_SIZE, comparator);
			
			if (subNode == subNodeReplacement)
				return this;
			
			// TODO: optimization if singleton element node is returned
			final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, bitIndex, subNodeReplacement);
			return new InplaceIndexNode<>(bitmap, valmap, nodesReplacement, this.size() - 1);
		} else {
			// it's an inplace value
			if (comparator.compare(nodes[valIndex], key) != 0)
				return this;

			// TODO: optimization if singleton element node is returned
			final Object[] nodesReplacement = ArrayUtils.arraycopyAndRemove(nodes, valIndex);
			return new InplaceIndexNode<>(bitmap & ~bitpos, valmap & ~bitpos, nodesReplacement, this.size() - 1);
		}
	}
	
	// TODO: ensure editability
	@Override
	public TrieSet<K> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator) {
		final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
		final int bitpos = (1 << mask);
		final int bitIndex = bitIndex(bitpos);
		final int valIndex = valIndex(bitpos);

		if ((bitmap & bitpos) == 0) {
			return this;
		}

		if ((valmap & bitpos) == 0) {
			// it's a TrieSet node, not a inplace value
			@SuppressWarnings("unchecked")
			final TrieSet<K> oldNode = (TrieSet<K>) nodes[bitIndex];
			TrieSet<K> newNode = oldNode.removed(mutator, key, hash, shift + BIT_PARTITION_SIZE, comparator);
			
			if (newNode == oldNode) {
				return oldNode;
			} else {
				InplaceIndexNode<K> editableNode = editAndSet(mutator, valIndex, newNode);
				editableNode.updateBitmaps(bitmap, valmap, cachedValmapBitCount);
				return editableNode;
			}
		} else {
			// it's an inplace value
			if (comparator.compare(nodes[valIndex], key) != 0) {
				return this;
			} else {
				// TODO: optimization if singleton element node is returned
				InplaceIndexNode<K> editableNode = editAndRemove(mutator, valIndex);
				editableNode.updateBitmaps(this.bitmap &= ~bitpos,
						this.valmap &= ~bitpos, cachedValmapBitCount - 1);
				return editableNode;
			}
		}
	}

	@Override
	public int size() {
		return cachedSize;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<K> iterator() {
		return new TrieSetIterator(this);
	}
	
	/**
	 * Iterator that first iterates over inlined-values and then
	 * continues depth first recursively.
	 */
	private static class TrieSetIterator implements Iterator {

		final Deque<Iterator<TrieSet>> nodeIterationStack;
		Iterator<?> valueIterator;
		
		TrieSetIterator(TrieSet trieSet) {			
			if (trieSet.hasValues()) {
				valueIterator = trieSet.valueIterator();
			} else {
				valueIterator = Collections.emptyIterator();
			}

			nodeIterationStack = new ArrayDeque<>();
			if (trieSet.hasNodes()) {
				nodeIterationStack.push(trieSet.nodeIterator());
			}
		}

		@Override
		public boolean hasNext() {
			while (true) {
				if (valueIterator.hasNext()) {
					return true;
				} else {						
					if (nodeIterationStack.isEmpty()) {
						return false;
					} else {
						if (nodeIterationStack.peek().hasNext()) {
							TrieSet trieSet = nodeIterationStack.peek().next();						
							
							if (trieSet.hasValues())
								valueIterator = trieSet.valueIterator();
							
							if (trieSet.hasNodes()) {
								nodeIterationStack.push(trieSet.nodeIterator());
							}
							continue;
						} else {
							nodeIterationStack.pop();
							continue;
						}
					}
				}
			}
		}

		@Override
		public Object next() {
			if (!hasNext()) throw new NoSuchElementException();
			return valueIterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}	

	@SuppressWarnings("unchecked")
	@Override
	Iterator<K> valueIterator() {
//		if (cachedValSize == 0) 
//			return Collections.emptyIterator();
//		else
			return (Iterator) ArrayIterator.of(nodes, 0, cachedValmapBitCount);
	}

	@SuppressWarnings("unchecked")
	@Override
	Iterator<TrieSet<K>> nodeIterator() {
//		if (cachedValSize == nodes.length)
//			return Collections.emptyIterator();
//		else
			return (Iterator) ArrayIterator.of(nodes, cachedValmapBitCount, nodes.length - cachedValmapBitCount);
	}

	@Override
	boolean hasValues() {	
		return cachedValmapBitCount != 0;
	}

	@Override
	boolean hasNodes() {
		return cachedValmapBitCount != nodes.length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		result = prime * result + bitmap;
		result = prime * result + valmap;
		result = prime * result + Arrays.hashCode(nodes);
		return result;
	}

	@Override
	public boolean equals(Object that) {
		if (null == that) {
			return false;
		}
		if (this == that) {
			return true;
		}
		if (getClass() != that.getClass()) {
			return false;
		}
		InplaceIndexNode other = (InplaceIndexNode) that;
		if (bitmap != other.bitmap) {
			return false;
		}
		if (valmap != other.valmap) {
			return false;
		}
		if (!Arrays.equals(nodes, other.nodes)) {
			return false;
		}
		return true;
	}

}

@SuppressWarnings("rawtypes")
/*package*/ final class HashCollisionNode<K> extends TrieSet<K> {

	private final K[] keys;
	private final int hash;

	HashCollisionNode(int hash, K[] keys) {
		this.keys = keys;
		this.hash = hash;
	}

	@Override
	public int size() {
		return keys.length;
	}

	@Override
	public Iterator<K> iterator() {
		return ArrayIterator.of(keys);
	}
	
//	@Override
//	public Iterator<K> flatIterator() {
//		return ArrayIterator.of(keys);
//	}

	@SuppressWarnings("unchecked")
	@Override
	Iterator<K> valueIterator() {
		return ArrayIterator.of(keys);
	}

	@SuppressWarnings("unchecked")
	@Override
	Iterator<TrieSet<K>> nodeIterator() {
		return Collections.emptyIterator(); 
	}
	
	@Override
	public boolean contains(Object key, int hash, int shift, Comparator<Object> comparator) {
		for (K k : keys) {
			if (comparator.compare(k, key) == 0)
				return true;
		}
		return false;
	}

	/**
	 * Inserts an object if not yet present. Note, that this implementation always
	 * returns a new immutable {@link TrieSet} instance.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TrieSet<K> updated(K key, int hash, int shift, Comparator comparator) {
		if (this.hash != hash)
			return mergeNodes((TrieSet) this, this.hash, key, hash, shift);

		if (contains(key, hash, shift, comparator))
			return this;

		final K[] keysNew = (K[]) ArrayUtils.arraycopyAndInsert(keys, keys.length, key);
		return new HashCollisionNode<>(hash, keysNew);
	}

	@Override
	TrieSet<K> updated(AtomicReference<Thread> mutator, K key, int hash,
			int shift, Comparator<Object> cmp) {
		// TODO specialize
		return updated(key, hash, shift, cmp);
	}

	/**
	 * Removes an object if present. Note, that this implementation always
	 * returns a new immutable {@link TrieSet} instance.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TrieSet<K> removed(K key, int hash, int shift, Comparator comparator) {
		// TODO: optimize in general
		// TODO: optimization if singleton element node is returned

		for (int i = 0; i < keys.length; i++) {
			if (comparator.compare(keys[i], key) == 0)
				return new HashCollisionNode<>(hash, (K[]) ArrayUtils.arraycopyAndRemove(keys, i));
		}
		return this;
	}
	
	@Override
	TrieSet<K> removed(AtomicReference<Thread> mutator, K key, int hash,
			int shift, Comparator<Object> comparator) {
		// TODO specialize
		return removed(key, hash, shift, comparator);
	}	

	@Override
	boolean hasValues() {
		return true;
	}

	@Override
	boolean hasNodes() {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		result = prime * result + hash;
		result = prime * result + Arrays.hashCode(keys);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		HashCollisionNode other = (HashCollisionNode) obj;
		
		if (hash != other.hash) {
			return false;
		}

		// not possible due to arbitrary order
//		if (!Arrays.equals(keys, other.keys)) {
//			return false;
//		}
		
		for (K key : keys) {
			if (!other.contains(key)) {
				return false;
			}
		}
		
		return true;
	}
	
}

/*
 * TODO: exchange TrieSet.equivalenceComparator() with standard equality operator
 */
class TransientTrieSet<E> implements TransientSet<E> {
	
	private AtomicReference<Thread> mutator; // TODO: initialize
	
	protected TrieSet<E> content;
	
	/*package*/ TransientTrieSet() {
		// allocated a new empty instance, because transient allows inplace modification.
		content = new InplaceIndexNode<>(0, 0, new TrieSet[0], 0);
	}
	
	@Override
	public boolean __insert(E e) {
		return __insertEquivalent(e, TrieSet.equivalenceComparator());
	}

	@Override
	public boolean __insertEquivalent(E e, Comparator<Object> cmp) {
		int sizeBeforeUpdate = content.size();
		content = content.updated(mutator, e, e.hashCode(), 0, cmp);
		
		return sizeBeforeUpdate != content.size();
	}

	@Override
	public boolean __remove(E e) {
		return __removeEquivalent(e, TrieSet.equivalenceComparator());
	}

	@Override
	public boolean __removeEquivalent(E e, Comparator<Object> cmp) {
		int sizeBeforeUpdate = content.size();
		content = content.removed(mutator, (E) e, e.hashCode(), 0, cmp);

		return sizeBeforeUpdate != content.size();
	}

	@Override
	public boolean __insertAll(Set<? extends E> set) {
		return __insertAllEquivalent(set, TrieSet.equivalenceComparator());
	}

	@Override
	public boolean __insertAllEquivalent(Set<? extends E> set,
			Comparator<Object> cmp) {
		boolean modified = false;

		for (E e : set)
			modified |= __insertEquivalent(e, cmp);
						
		return modified;	
	}	
	
//	@Override
//	public boolean removeAll(Collection<?> c) {
//		boolean modified = false;
//
//		for (Object o : c)
//			modified |= remove(o);
//						
//		return modified;
//	}
//
//	@Override
//	public boolean retainAll(Collection<?> c) {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public void clear() {
//		// allocated a new empty instance, because transient allows inplace modification.
//		content = new InplaceIndexNode<>(0, 0, new TrieSet[0], 0);
//	}	
//
//	@Override
//	public Iterator<E> iterator() {
//		return content.iterator();
//	}
//
//	@Override
//	public int size() {
//		return content.size();
//	}
//
//	@Override
//	public boolean isEmpty() {
//		return content.isEmpty();
//	}
//
//	@Override
//	public boolean contains(Object o) {
//		return content.contains(o);
//	}
//	
//	@Override
//	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public Object[] toArray() {
//		return content.toArray();
//	}
//
//	@Override
//	public <T> T[] toArray(T[] a) {
//		return content.toArray(a);
//	}
//
//	@Override
//	public boolean containsAll(Collection<?> c) {
//		return content.containsAll(c);
//	}

	@Override
	public boolean equals(Object o) {
		return content.equals(o);
	}
	
	@Override
	public int hashCode() {
		return content.hashCode();
	}

	@Override
	public String toString() {
		return content.toString();
	}
	
	@Override
	public ImmutableSet<E> freeze() {
		return content;
	}
	
}
