package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.ShortDescriptionBean;


@Remote
public interface ShortDescriptionsRemote {

	public static final String NAME = "ShortDescriptions";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/ShortDescriptions";
	
	public ShortDescriptionBean find(long sdId);

	public ShortDescriptionBean create(ShortDescriptionBean sd);

	public ShortDescriptionBean update(ShortDescriptionBean sd);

	public void delete(long sdId);

}