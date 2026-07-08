/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.Marker;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.HandlerIteration.MarkerDecision;

public class HandlerIterationTest {

	@Test
	public void testRunEverything() {
		IndexingItemProgress item1 = mock(IndexingItemProgress.class, "Item1");
		IndexingItemProgress item2 = mock(IndexingItemProgress.class, "Item2");
		Set<IndexingItemProgress> changeset1 = new LinkedHashSet<IndexingItemProgress>();
		changeset1.add(item1);
		changeset1.add(item2);
		
		List<HandlerCall> calls = new LinkedList<HandlerCall>();
		List<IndexingItemHandler> handlers = new LinkedList<IndexingItemHandler>();
		handlers.add(new Handler1(calls));
		handlers.add(new Handler2(calls));
		handlers.add(new Marker1(calls));
		handlers.add(new Handler3(calls));
		
		IndexingUnit unit = new IndexingUnit(changeset1, handlers);
		
		HandlerIteration runeverything = new HandlerIteration(new MarkerDecision() {
			@Override
			public boolean before(IndexingItemHandler handler, IndexingItemProgress item) {
				return true;
			}
			
			@Override
			public boolean before(Marker marker) {
				return true;
			}
			
			@Override
			public boolean after(Marker marker) {
				return true;
			}
		});
		
		runeverything.proceed(unit);
		
		System.out.println(calls);
		
		Iterator<HandlerCall> c = calls.iterator();
		assertEquals("Handler1+Item1", c.next().toString());
		assertEquals("Should proceed with first item until a marker is seen",
				"Handler2+Item1", c.next().toString());
		assertEquals("handle should be called for markers too",
				"Marker1+Item1", c.next().toString());

		assertEquals("before next marker both items should run",
				"Handler1+Item2", c.next().toString());
		assertEquals("Handler2+Item2", c.next().toString());
		assertEquals("Marker1+Item2", c.next().toString());
		
		assertEquals("should then trigger marker",
				"Marker1", c.next().toString());
		
		assertEquals("should then proceed to next handler",
				"Handler3+Item1", c.next().toString());
		assertEquals("should then proceed to next handler",
				"Handler3+Item2", c.next().toString());
		assertFalse(c.hasNext());
	}
	
	@Test
	public void testIgnoreHandler() {
		IndexingItemProgress item1 = mock(IndexingItemProgress.class, "Item1");
		IndexingItemProgress item2 = mock(IndexingItemProgress.class, "Item2");
		Set<IndexingItemProgress> changeset1 = new LinkedHashSet<IndexingItemProgress>();
		changeset1.add(item1);
		changeset1.add(item2);
		
		List<HandlerCall> calls = new LinkedList<HandlerCall>();
		List<IndexingItemHandler> handlers = new LinkedList<IndexingItemHandler>();
		handlers.add(new Handler1(calls));
		handlers.add(new Handler2(calls));
		handlers.add(new Marker1(calls));
		handlers.add(new Handler3(calls));
		
		IndexingUnit unit = new IndexingUnit(changeset1, handlers);
		
		HandlerIteration skipHandler2 = new HandlerIteration(new MarkerDecision() {
			@Override
			public boolean before(IndexingItemHandler handler, IndexingItemProgress item) {
				return !(handler instanceof Handler2);
			}
			
			@Override
			public boolean before(Marker marker) {
				return true;
			}
			
			@Override
			public boolean after(Marker marker) {
				return true;
			}
		});
		
		skipHandler2.proceed(unit);
		
		System.out.println(calls);
		
		Iterator<HandlerCall> c = calls.iterator();
		assertEquals("Handler1+Item1", c.next().toString());
		assertEquals("should skip handler2",
				"Marker1+Item1", c.next().toString());
		
		assertEquals("should stop at marker and proceed with next item",
				"Handler1+Item2", c.next().toString());
		assertEquals("should skip handler2 for item2 also",
				"Marker1+Item2", c.next().toString());
		
		assertEquals("should then trigger marker",
				"Marker1", c.next().toString());
		
		assertEquals("should then proceed to next handler",
				"Handler3+Item1", c.next().toString());
		assertEquals("should then proceed to next handler",
				"Handler3+Item2", c.next().toString());
		assertFalse(c.hasNext());
	}
	
	@Test
	public void testSkipMarker() {
		IndexingItemProgress item1 = mock(IndexingItemProgress.class, "Item1");
		IndexingItemProgress item2 = mock(IndexingItemProgress.class, "Item2");
		Set<IndexingItemProgress> changeset1 = new LinkedHashSet<IndexingItemProgress>();
		changeset1.add(item1);
		changeset1.add(item2);
		
		List<HandlerCall> calls = new LinkedList<HandlerCall>();
		List<IndexingItemHandler> handlers = new LinkedList<IndexingItemHandler>();
		handlers.add(new Handler1(calls));
		handlers.add(new Handler2(calls));
		handlers.add(new Marker1(calls));
		handlers.add(new Handler3(calls));
		handlers.add(new Marker2(calls));
		handlers.add(new Handler4(calls));
		
		IndexingUnit unit = new IndexingUnit(changeset1, handlers);
		
		HandlerIteration skipMarker1 = new HandlerIteration(new MarkerDecision() {
			@Override
			public boolean before(IndexingItemHandler handler, IndexingItemProgress item) {
				return true;
			}
			
			@Override
			public boolean before(Marker marker) {
				return !(marker instanceof Marker1);
			}
			
			@Override
			public boolean after(Marker marker) {
				return true;
			}
		});
		
		try {
			skipMarker1.proceed(unit);
		} finally {
			System.out.println(calls);
		}
		
		Iterator<HandlerCall> c = calls.iterator();
		assertEquals("Handler1+Item1", c.next().toString());
		assertEquals("Should proceed with first item until a marker is seen",
				"Handler2+Item1", c.next().toString());
		assertEquals("Decision is to ignore the marker, should be flagged before first item (unlike trigger which is after last item)",
				"Marker1+ignore", c.next().toString());
		assertEquals("Marker should be skipped so iteration should proceed through the item",
				"Handler3+Item1", c.next().toString());
		assertEquals("If iteration should proceed on ignored markers, it means that we'll ask for a decision for all markers at the first item reaching them",
				"Marker2+Item1", c.next().toString());

		assertEquals("Marker2 isn't ignored so iteration should go to Item2 next",
				"Handler1+Item2", c.next().toString());
		assertEquals("Handler2+Item2", c.next().toString());
		assertEquals("Handler3+Item2", c.next().toString());
		assertEquals("Marker2+Item2", c.next().toString());
		
		assertEquals("After Item2 all is level so marker2 should be triggered",
				"Marker2", c.next().toString());
		assertEquals("Then there's a handler left",
				"Handler4+Item1", c.next().toString());
		assertEquals("Handler4+Item2", c.next().toString());
		assertFalse(c.hasNext());
	}
	
	@Test
	public void testSingleItem() {
		IndexingItemProgress item1 = mock(IndexingItemProgress.class, "Item1");
		Set<IndexingItemProgress> changeset1 = new LinkedHashSet<IndexingItemProgress>();
		changeset1.add(item1);
		
		List<HandlerCall> calls = new LinkedList<HandlerCall>();
		List<IndexingItemHandler> handlers = new LinkedList<IndexingItemHandler>();
		handlers.add(new Handler1(calls));
		handlers.add(new Handler2(calls));
		handlers.add(new Handler3(calls));
		handlers.add(new Marker1(calls));
		handlers.add(new Marker2(calls));
		handlers.add(new Handler4(calls));
		
		IndexingUnit unit = new IndexingUnit(changeset1, handlers);
		
		HandlerIteration skipsome = new HandlerIteration(new MarkerDecision() {
			@Override
			public boolean before(IndexingItemHandler handler, IndexingItemProgress item) {
				return !(handler instanceof Handler2);
			}
			
			@Override
			public boolean before(Marker marker) {
				return !(marker instanceof Marker1);
			}
			
			@Override
			public boolean after(Marker marker) {
				return true;
			}
		});
		
		skipsome.proceed(unit);
		
		System.out.println(calls);
		
		Iterator<HandlerCall> c = calls.iterator();
		assertEquals("Handler1+Item1", c.next().toString());
		assertEquals("Handler3+Item1", c.next().toString());
		assertEquals("Marker1+ignore", c.next().toString());
		assertEquals("Marker2+Item1", c.next().toString());
		assertEquals("Marker2", c.next().toString());
		assertEquals("Handler4+Item1", c.next().toString());
		assertFalse(c.hasNext());
	}
	
	private class HandlerCall {
		
		IndexingItemHandler handler;
		IndexingItemProgress item;
		boolean isIgnore;
		
		HandlerCall(IndexingItemHandler handler, IndexingItemProgress progress) {
			this.handler = handler;
			this.item = progress;
			this.isIgnore = false;			
		}
		
		HandlerCall(Marker marker, boolean isTrigger) {
			this.handler = marker;
			this.item = null;
			this.isIgnore = !isTrigger;
		}

		@Override
		public String toString() {
			return ""
					+ handler.getClass().getSimpleName()
					+ (isIgnore ? "+ignore" : "")
					+ (item != null ? "+" + item : "");
		}
		
	}
	
	private class TraceHandler implements IndexingItemHandler {

		protected Collection<HandlerCall> calls;

		TraceHandler(Collection<HandlerCall> calls) {
			this.calls = calls;
		}
		
		@Override
		public void handle(IndexingItemProgress progress) {
			calls.add(new HandlerCall(this, progress));
		}

		@Override
		public Set<Class<? extends IndexingItemHandler>> getDependencies() {
			return null;
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName();
		}
		
	}
	
	private class TraceMarker extends TraceHandler implements Marker {

		TraceMarker(Collection<HandlerCall> calls) {
			super(calls);
		}
		
		@Override
		public void trigger() {
			calls.add(new HandlerCall(this, true));
		}

		@Override
		public void ignore() {
			calls.add(new HandlerCall(this, false));
		}
		
	}
	
	private class Handler1 extends TraceHandler {  Handler1(Collection<HandlerCall> calls) { super(calls); } }
	private class Handler2 extends TraceHandler {  Handler2(Collection<HandlerCall> calls) { super(calls); } }
	private class Handler3 extends TraceHandler {  Handler3(Collection<HandlerCall> calls) { super(calls); } }
	private class Handler4 extends TraceHandler {  Handler4(Collection<HandlerCall> calls) { super(calls); } }
	private class Marker1 extends TraceMarker {  Marker1(Collection<HandlerCall> calls) { super(calls); } }
	private class Marker2 extends TraceMarker {  Marker2(Collection<HandlerCall> calls) { super(calls); } }

}
