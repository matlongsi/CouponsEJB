package com.coupon.common.session.remote;

import java.util.List;

import javax.ejb.Remote;

import com.coupon.common.GlobalTradeIdentificationNumber;
import com.coupon.common.bean.GlobalTradeIdentificationNumberBean;


@Remote
public interface GlobalTradeIdentificationNumbersRemote {

	public static final String NAME = "GlobalTradeIdentificationNumbers";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/GlobalTradeIdentificationNumbers";
	
	public List<GlobalTradeIdentificationNumberBean> findPage(
													long companyPrefix,
													int start,
													int size);

	public GlobalTradeIdentificationNumberBean find(long gtinId);

	public GlobalTradeIdentificationNumberBean findByNumber(GlobalTradeIdentificationNumber gtin);

	public GlobalTradeIdentificationNumberBean create(GlobalTradeIdentificationNumber gtin);

	public GlobalTradeIdentificationNumberBean update(GlobalTradeIdentificationNumberBean gtin);

	public void delete(long gtinId);

}