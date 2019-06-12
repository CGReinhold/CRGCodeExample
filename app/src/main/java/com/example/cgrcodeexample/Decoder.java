package com.example.cgrcodeexample;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class Decoder {
    Map<String,String> numericTable = new HashMap<>();
    Map<String,String> alphanumericTable = new HashMap<>();

    public Decoder() {
        numericTable.put("00", "");
        numericTable.put("01", "0");
        numericTable.put("02", "1");
        numericTable.put("03", "2");
        numericTable.put("10", "3");
        numericTable.put("11", "4");
        numericTable.put("12", "5");
        numericTable.put("13", "6");
        numericTable.put("20", "7");
        numericTable.put("21", "8");
        numericTable.put("22", "9");
        numericTable.put("23", "+");
        numericTable.put("30", "-");
        numericTable.put("31", ".");
        numericTable.put("32", "/");
        numericTable.put("33", "_");

        alphanumericTable.put("000", "0");
        alphanumericTable.put("001", "1");
        alphanumericTable.put("002", "2");
        alphanumericTable.put("003", "3");
        alphanumericTable.put("010", "4");
        alphanumericTable.put("011", "5");
        alphanumericTable.put("012", "6");
        alphanumericTable.put("013", "7");
        alphanumericTable.put("020", ".");
        alphanumericTable.put("021", "8");
        alphanumericTable.put("022", "9");
        alphanumericTable.put("023", "a");
        alphanumericTable.put("030", "b");
        alphanumericTable.put("031", "c");
        alphanumericTable.put("032", "d");
        alphanumericTable.put("033", "e");
        alphanumericTable.put("100", "f");
        alphanumericTable.put("101", "g");
        alphanumericTable.put("102", "h");
        alphanumericTable.put("103", "i");
        alphanumericTable.put("110", "j");
        alphanumericTable.put("111", "k");
        alphanumericTable.put("112", "l");
        alphanumericTable.put("113", "m");
        alphanumericTable.put("120", "n");
        alphanumericTable.put("121", "o");
        alphanumericTable.put("122", "p");
        alphanumericTable.put("123", "q");
        alphanumericTable.put("130", "r");
        alphanumericTable.put("131", "s");
        alphanumericTable.put("132", "t");
        alphanumericTable.put("133", "u");
        alphanumericTable.put("200", "v");
        alphanumericTable.put("201", "w");
        alphanumericTable.put("202", "x");
        alphanumericTable.put("203", "y");
        alphanumericTable.put("210", "z");
        alphanumericTable.put("211", "A");
        alphanumericTable.put("212", "B");
        alphanumericTable.put("213", "C");
        alphanumericTable.put("220", "D");
        alphanumericTable.put("221", "E");
        alphanumericTable.put("222", "F");
        alphanumericTable.put("223", "G");
        alphanumericTable.put("230", "H");
        alphanumericTable.put("231", "I");
        alphanumericTable.put("232", "J");
        alphanumericTable.put("233", "K");
        alphanumericTable.put("300", "L");
        alphanumericTable.put("301", "M");
        alphanumericTable.put("302", "N");
        alphanumericTable.put("303", "O");
        alphanumericTable.put("310", "P");
        alphanumericTable.put("311", "Q");
        alphanumericTable.put("312", "R");
        alphanumericTable.put("313", "S");
        alphanumericTable.put("320", "T");
        alphanumericTable.put("321", "U");
        alphanumericTable.put("322", "V");
        alphanumericTable.put("323", "W");
        alphanumericTable.put("330", "X");
        alphanumericTable.put("331", "Y");
        alphanumericTable.put("332", "Z");
        alphanumericTable.put("333", "/");
    }

    public String decode(String encoded) {
        String result = "";
        if (encoded.length() > 1) {
            char linkChar = encoded.charAt(0);
            char codeTypeChar = encoded.charAt(1);
            encoded = encoded.substring(2);

            switch (codeTypeChar)
            {
                case '0':
                    result = decodeNumeric(encoded);
                    break;
                case '1':
                    result = decodeAlphanumeric(encoded);
                    break;
                case '2':
                    break;
                case '3':
                    break;
            }

            switch (linkChar)
            {
                case '1':
                    result = "http://" + result;
                    break;
                case '2':
                    result = "https://" + result;
                    break;
                case '3':
                    result = "https://cgrco.de/" + result;
            }
        }

        Log.println(Log.INFO, "current", result);
        return result;
    }

    public String decodeNumeric(String encoded)
    {
        String result = "";
        String current = "";

        while (encoded.length() > 0)
        {
            current += encoded.charAt(0);
            encoded = encoded.substring(1);
            if (current.length() == 2)
            {
                result += numericTable.get(current);
                current = "";
            }
        }

        result = result.replaceAll("^0+", "");

        return result;
    }

    public String decodeAlphanumeric(String encoded)
    {
        String result = "";
        String current = "";

        while (encoded.length() > 0)
        {
            current += encoded.charAt(0);
            encoded = encoded.substring(1);
            if (current.length() == 3)
            {
                result += alphanumericTable.get(current);
                current = "";
            }
        }

        result = result.replaceAll("^\\.+", "");

        return result;
    }
}
