package me.jasonbaik.loadtester.reporter;

import java.util.List;

public abstract class Reporter<T1, T2 extends Reportable<T1>> {

	public abstract void report(List<T2> reportables);

}
