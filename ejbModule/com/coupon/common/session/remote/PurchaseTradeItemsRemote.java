package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.PurchaseTradeItemBean;


@Remote
public interface PurchaseTradeItemsRemote {

	public static final String NAME = "PurchaseRequirementTradeItems";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/PurchaseRequirementTradeItems";
	
	public PurchaseTradeItemBean find(long ptiId);

	public PurchaseTradeItemBean create(PurchaseTradeItemBean pti);

	public PurchaseTradeItemBean update(PurchaseTradeItemBean pti);

	public void delete(long ptiId);

}