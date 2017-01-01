package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.AwarderDetailBean;


@Remote
public interface AwarderDetailsRemote {

	public static final String NAME = "AwarderDetails";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/AwarderDetails";
	
	public AwarderDetailBean find(long oadId);

	public AwarderDetailBean create(AwarderDetailBean oad);

	public AwarderDetailBean update(AwarderDetailBean oad);

	public void delete(long adId);

}