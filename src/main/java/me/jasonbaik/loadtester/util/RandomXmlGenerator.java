package me.jasonbaik.loadtester.util;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.RandomStringUtils;

public class RandomXmlGenerator {

	public static void main(String[] args) {
		try {
			System.out.println(new String(generate(200)));
			System.out.println(new String(generate(200)).getBytes().length);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static byte[] generate(int byteLength) throws JAXBException {
		if (byteLength < 100) {
			throw new IllegalArgumentException("Byte length must be >=100");
		}

		XmlItem xmlItem = new XmlItem();
		xmlItem.setId(Long.parseLong(RandomStringUtils.randomNumeric(8)));
		xmlItem.setName(RandomStringUtils.randomAlphanumeric(10));
		xmlItem.setPayload(RandomStringUtils.randomAlphanumeric(byteLength - 78));

		JAXBContext context = JAXBContext.newInstance(XmlItem.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		m.marshal(xmlItem, os);
		return os.toByteArray();
	}
}
