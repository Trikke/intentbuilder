package be.trikke.intentbuilder;

import javax.lang.model.element.Element;

public class FoundPath {

	private final String name;
	private final String path;
	private final Element element;

	public FoundPath(String name, String path, Element element) {
		this.name = name;
		this.path = path;
		this.element = element;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public Element getElement() {
		return element;
	}
}
