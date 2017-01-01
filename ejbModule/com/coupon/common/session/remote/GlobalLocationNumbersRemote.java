package com.coupon.common.session.remote;

import java.util.List;

import javax.ejb.Remote;

import com.coupon.common.GlobalLocationNumber;
import com.coupon.common.bean.GlobalLocationNumberBean;


@Remote
public interface GlobalLocationNumbersRemote {

	public static final String NAME = "GlobalLocationNumbers";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/GlobalLocationNumbers";
	
	public List<GlobalLocationNumberBean> findPage(
											long companyPrefix,
											int start,
											int size);

	public GlobalLocationNumberBean find(long glnId);

	public GlobalLocationNumberBean findByNumber(GlobalLocationNumber gln);

	public GlobalLocationNumberBean create(GlobalLocationNumber gln);

	public GlobalLocationNumberBean update(GlobalLocationNumberBean gln);

	public void delete(long glnId);

}