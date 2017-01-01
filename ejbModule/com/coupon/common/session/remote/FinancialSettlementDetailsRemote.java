package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.FinancialSettlementDetailBean;


@Remote
public interface FinancialSettlementDetailsRemote {

	public static final String NAME = "FinancialSettlementDetails";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/FinancialSettlementDetails";
	
	public FinancialSettlementDetailBean find(long fsdId);

	public FinancialSettlementDetailBean create(FinancialSettlementDetailBean fsd);

	public FinancialSettlementDetailBean update(FinancialSettlementDetailBean fsd);

	public void delete(long fsdId);

}