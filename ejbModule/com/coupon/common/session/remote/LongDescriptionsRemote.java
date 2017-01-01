package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.LongDescriptionBean;


@Remote
public interface LongDescriptionsRemote {

	public static final String NAME = "LongDescriptions";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/LongDescriptions";
	
	public LongDescriptionBean find(long ldId);

	public LongDescriptionBean create(LongDescriptionBean ld);

	public LongDescriptionBean update(LongDescriptionBean ld);

	public void delete(long ldId);

}