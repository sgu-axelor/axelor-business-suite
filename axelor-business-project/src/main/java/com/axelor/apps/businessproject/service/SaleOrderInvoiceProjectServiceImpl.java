/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.businessproject.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.PaymentCondition;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.invoice.InvoiceService;
import com.axelor.apps.base.db.AppBusinessProject;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PriceList;
import com.axelor.apps.businessproject.service.app.AppBusinessProjectService;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.supplychain.service.SaleOrderInvoiceServiceImpl;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SaleOrderInvoiceProjectServiceImpl extends SaleOrderInvoiceServiceImpl {

  private AppBusinessProjectService appBusinessProjectService;

  @Inject
  public SaleOrderInvoiceProjectServiceImpl(
      AppSupplychainService appSupplychainService,
      SaleOrderRepository saleOrderRepo,
      InvoiceRepository invoiceRepo,
      InvoiceService invoiceService,
      AppBusinessProjectService appBusinessProjectService) {
    super(appSupplychainService, saleOrderRepo, invoiceRepo, invoiceService);
    this.appBusinessProjectService = appBusinessProjectService;
  }

  @Transactional
  public Invoice mergeInvoice(
      List<Invoice> invoiceList,
      Company company,
      Currency currency,
      Partner partner,
      Partner contactPartner,
      PriceList priceList,
      PaymentMode paymentMode,
      PaymentCondition paymentCondition,
      SaleOrder saleOrder,
      Project project)
      throws AxelorException {
    Invoice invoiceMerged =
        super.mergeInvoice(
            invoiceList,
            company,
            currency,
            partner,
            contactPartner,
            priceList,
            paymentMode,
            paymentCondition,
            saleOrder);
    if (project != null
        && !appBusinessProjectService.getAppBusinessProject().getProjectInvoiceLines()) {
      invoiceMerged.setProject(project);
      for (InvoiceLine invoiceLine : invoiceMerged.getInvoiceLineList()) {
        invoiceLine.setProject(project);
      }
    }
    return invoiceMerged;
  }

  @Override
  public Map<String, Integer> getInvoicingWizardOperationDomain(SaleOrder saleOrder) {
    Map<String, Integer> contextValues = super.getInvoicingWizardOperationDomain(saleOrder);
    AppBusinessProject appBusinessProject = appBusinessProjectService.getAppBusinessProject();
    boolean canInvoiceTimesheet = appBusinessProject.getEnableToInvoiceTimesheet();
    boolean canInvoiceExpense = appBusinessProject.getEnableToInvoiceExpense();

    contextValues.put("invoiceTs", canInvoiceTimesheet ? SaleOrderRepository.INVOICE_TIMESHEET : 0);
    contextValues.put(
        "invoiceExpense", canInvoiceExpense ? SaleOrderRepository.INVOICE_EXPENSE : 0);

    return contextValues;
  }
  
  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public Invoice generateInvoice(
	      SaleOrder saleOrder,
	      int operationSelect,
	      BigDecimal amount,
	      boolean isPercent,
	      Map<Long, BigDecimal> qtyToInvoiceMap)
	      throws AxelorException {
	
	    Invoice invoice = super.generateInvoice(saleOrder, operationSelect, amount, isPercent, qtyToInvoiceMap);
	    invoice.setProject(saleOrder.getProject());
	    if (operationSelect == SaleOrderRepository.INVOICE_ALL 
	    		|| operationSelect == SaleOrderRepository.INVOICE_PART) {
	    	for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
	    		SaleOrderLine saleOrderLine = invoiceLine.getSaleOrderLine();
	    		if (saleOrderLine != null) {
	    			invoiceLine.setProject(saleOrderLine.getProject());
	    		}
	    	}
	    }
	    else {
	    	for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
	    		invoiceLine.setProject(saleOrder.getProject());
	    	}
	    }
	    
	    return invoice;
}
}
