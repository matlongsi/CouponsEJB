package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.AwarderPointOfSaleBean;


@Remote
public interface AwarderPointOfSalesRemote {

	public static final String NAME = "AwarderPointOfSales";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/AwarderPointOfSales";
	
	public AwarderPointOfSaleBean find(long aposId);

	public AwarderPointOfSaleBean create(AwarderPointOfSaleBean apos);

	public AwarderPointOfSaleBean update(AwarderPointOfSaleBean apos);

	public void delete(long aposId);

}