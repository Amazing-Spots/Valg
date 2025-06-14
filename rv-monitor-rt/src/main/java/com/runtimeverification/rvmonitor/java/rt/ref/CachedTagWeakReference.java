package com.runtimeverification.rvmonitor.java.rt.ref;

/**
 * This class should be no longer used.
 * 
 */
public class CachedTagWeakReference extends CachedWeakReference {
  	private long disabled = -1;
	private long tau = -1;
	
	public long getDisabled() {
		return this.disabled;
	}
	
	public void setDisabled(long d) {
		this.disabled = d;
	}
	
	public long getTau() {
		return this.tau;
	}
	
	public void setTau(long t) {
		this.tau = t;
	}
	
	public CachedTagWeakReference(Object ref) {
		this(ref, System.identityHashCode(ref));
	}
	
	public CachedTagWeakReference(Object ref, int hashval) {
		super(ref, hashval);
	}
}