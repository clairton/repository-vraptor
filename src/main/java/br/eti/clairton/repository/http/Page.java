package br.eti.clairton.repository.http;

public class Page {
	public final Integer offset;
	public final Integer limit;

	public Page(final Integer offet, final Integer limit) {
		this.offset = offet;
		this.limit = limit;
	}
}