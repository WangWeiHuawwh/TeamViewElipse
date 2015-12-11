package com.example.teamview;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;

/**
 * Created by Administrator on 2015/12/1.
 */
public class XMLParse {
	public static XmlPullParser parser = Xml.newPullParser();

	public static String parseResponseCheck(InputStream inStream)
			throws Exception {
		parser.setInput(inStream, "UTF-8");
		int eventType = parser.getEventType();// 产生第一个事件
		while (eventType != XmlPullParser.END_DOCUMENT) {
			// 只要不是文档结束事件
			switch (eventType) {
			case XmlPullParser.START_TAG:
				String name = parser.getName();// 获取解析器当前指向的元素的名称
				if ("string".equals(name)) {
					return parser.nextText();
				}else if("dateTime".equals(name))
				{
					return parser.nextText();
				}
				break;
			// // 判断当前事件是否为文档开始事件
			// case XmlPullParser.START_DOCUMENT:
			// mList = new ArrayList<Beauty>(); // 初始化books集合
			// break;
			// // 判断当前事件是否为标签元素开始事件
			// case XmlPullParser.START_TAG:
			// if (xpp.getName().equals("beauty")) { // 判断开始标签元素是否是book
			// beauty = new Beauty();
			// } else if (xpp.getName().equals("name")) {
			// eventType = xpp.next();//让解析器指向name属性的值
			// // 得到name标签的属性值，并设置beauty的name
			// beauty.setName(xpp.getText());
			// } else if (xpp.getName().equals("age")) { // 判断开始标签元素是否是book
			// eventType = xpp.next();//让解析器指向age属性的值
			// // 得到age标签的属性值，并设置beauty的age
			// beauty.setAge(xpp.getText());
			// }
			// break;
			//
			// // 判断当前事件是否为标签元素结束事件
			// case XmlPullParser.END_TAG:
			// if (xpp.getName().equals("beauty")) { // 判断结束标签元素是否是book
			// mList.add(beauty); // 将book添加到books集合
			// beauty = null;
			// }
			// break;
			}
			eventType = parser.next();
		}
		return null;
	}
	
}
