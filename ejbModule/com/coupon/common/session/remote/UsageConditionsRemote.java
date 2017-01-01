package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.UsageConditionBean;


@Remote
public interface UsageConditionsRemote {

	public static final String NAME = "UsageConditions";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/UsageConditions";
	
	public UsageConditionBean find(long ucId);

	public UsageConditionBean create(UsageConditionBean uc);

	public UsageConditionBean update(UsageConditionBean uc);

	public void delete(long ucId);

}