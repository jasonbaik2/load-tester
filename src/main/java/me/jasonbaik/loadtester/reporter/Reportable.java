package me.jasonbaik.loadtester.reporter;

import java.util.ArrayList;

public interface Reportable<T> {

	public ArrayList<T> report() throws InterruptedException;

	public void destroy() throws Exception;

}
