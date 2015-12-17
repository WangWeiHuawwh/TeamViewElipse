package com.example.teamview;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "user")
public class User {
	@Column(name = "id", isId = true)
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "midtext")
	String midtext;

	public String getmIdText() {
		return midtext;
	}

	public void setmIdText(String mIdText) {
		this.midtext = mIdText;
	}

}
