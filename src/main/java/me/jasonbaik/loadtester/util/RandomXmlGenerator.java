package me.jasonbaik.loadtester.util;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class RandomXmlGenerator {

	public static void main(String[] args) {
		try {
			generate(10);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static byte[] generate(int byteLength) throws JAXBException {
		XmlItem item = new XmlItem(byteLength);
		byte[] xmlText = objectToXML(item).getBytes();
		return xmlText;
	}

	private static String objectToXML(XmlItem object) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(XmlItem.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		StringWriter xmlText = new StringWriter();
		m.marshal(object, xmlText);
		return xmlText.toString();
	}

}
