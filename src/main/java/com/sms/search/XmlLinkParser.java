package com.sms.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * parse an xml from an input stream an return the 1st 10 result links
 * use SAX parser 
 */
public class XmlLinkParser {

	/**
	 * @param xmlIS - the input stream with xml
	 * @return the 1st 10 links
	 */
	public List<String> parse(InputStream xmlIS) {
		SAXPars saxp = new SAXPars(); 
		try (InputStream inputStream = xmlIS) {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(inputStream, saxp);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	    return saxp.getResult();
	}
	
	private class SAXPars extends DefaultHandler {
		private final String linkLocation = "rss:channel:item:link:";
		private StringBuilder path = new StringBuilder();
		private List<String> result = new ArrayList<>();
		private StringBuilder text;

		public List<String> getResult() {
			// return only the first 10 results
			return result.subList(0, 10);
		}

		@Override 
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			// build path to be able to distinguish an item links from other links
			path.append(qName + ":");
			text = new StringBuilder();
		} 

		@Override 
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
			if (path.toString().equals(linkLocation)) {
				result.add(text.toString());
			}
			// update the path on exit 
			path.delete(path.length() - qName.length() - 1, path.length());
		} 

		@Override 
		public void characters(char[] ch, int start, int length) throws SAXException { 
			// get only links located in the items
			if (path.toString().equals(linkLocation)) {
				text.append(new String(ch, start, length));
			}
		} 
	}
}
