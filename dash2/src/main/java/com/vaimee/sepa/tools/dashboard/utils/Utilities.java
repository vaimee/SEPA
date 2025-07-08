package com.vaimee.sepa.tools.dashboard.utils;

import java.net.URI;

import javax.xml.datatype.DatatypeFactory;

public class Utilities {
	public static boolean checkType(String value, String type) {
		if (type == null)
			return true;

		try {
			switch (type) {
			case "URI":
				if (value.equals(""))
					return false;
				URI check = new URI(value);
				if (check.getScheme() == null)
					return false;
				break;
			case "xsd:base64Binary":
				Integer.parseInt(value, 16);
				break;
			case "xsd:boolean":
				if (!(value.equals("true") || value.equals("false") || value.equals("0") || value.equals("1")))
					return false;
				break;
			case "xsd:byte":
				Byte.parseByte(value);
				break;
			case "xsd:date":
			case "xsd:dateTime":
			case "xsd:time":
				DatatypeFactory.newInstance().newXMLGregorianCalendar(value);
				break;
			case "xsd:decimal":
				new java.math.BigDecimal(value);
				break;
			case "xsd:double":
				Double.parseDouble(value);
				break;
			case "xsd:float":
				Float.parseFloat(value);
				break;
			case "xsd:int":
				Integer.parseInt(value);
				break;
			case "xsd:integer":
				new java.math.BigInteger(value);
				break;
			case "xsd:long":
				Long.parseLong(value);
				break;
			case "xsd:short":
				Short.parseShort(value);
				break;
			case "xsd:QName":
				new javax.xml.namespace.QName(value);
				break;
			}
		} catch (Exception e) {
			return false;
		}

		return true;
	}

}
