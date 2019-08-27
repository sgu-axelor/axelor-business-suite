package com.axelor.apps.gst.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.service.AccountManagementAccountService;
import com.axelor.apps.account.service.AnalyticMoveLineService;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.invoice.InvoiceLineService;
import com.axelor.apps.account.service.invoice.generator.line.InvoiceLineManagement;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.service.CurrencyService;
import com.axelor.apps.base.service.PriceListService;
import com.axelor.apps.businessproject.service.InvoiceLineProjectServiceImpl;
import com.axelor.apps.purchase.service.PurchaseProductService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;

public class GstInvoiceLineServiceImpl extends InvoiceLineProjectServiceImpl implements GstInvoiceLineService{

  InvoiceLineService invoiceLineService;
  
  @Inject
  public GstInvoiceLineServiceImpl(
      CurrencyService currencyService,
      PriceListService priceListService,
      AppAccountService appAccountService,
      AnalyticMoveLineService analyticMoveLineService,
      AccountManagementAccountService accountManagementAccountService,
      PurchaseProductService purchaseProductService, InvoiceLineService invoiceLineService) {
    super(
        currencyService,
        priceListService,
        appAccountService,
        analyticMoveLineService,
        accountManagementAccountService,
        purchaseProductService);
    this.invoiceLineService =invoiceLineService;
  }

  @Override
  public Map<String, Object> fillProductInformation(Invoice invoice, InvoiceLine invoiceLine)
      throws AxelorException {
    Map<String, Object> productInformation = super.fillProductInformation(invoice, invoiceLine);
    productInformation.put("gstRate", invoiceLine.getProduct().getGstRate());
    productInformation.put("hsbn", invoiceLine.getProduct().getHsbn());
    if (productInformation.get("taxRate") == null && productInformation.get("gstRate") != null) {
      productInformation.put("taxRate", productInformation.get("gstRate"));
    }
    return productInformation;
  }

  public Map<String, Object> calculateGst(
      Invoice invoice, InvoiceLine invoiceLine, Map<String, Object> invoiceLineInformation) {

    Address invoiceAddress = invoice.getAddress();
    Address companyAddress = invoice.getCompany().getAddress();
    BigDecimal price = (BigDecimal) invoiceLineInformation.get("exTaxTotal");
    BigDecimal tax = invoiceLine.getTaxRate();
    final BigDecimal Two = new BigDecimal("2");

    invoiceLineInformation.put("igst", BigDecimal.ZERO);
    invoiceLineInformation.put("cgst", BigDecimal.ZERO);
    invoiceLineInformation.put("sgst", BigDecimal.ZERO);
    if (tax != null) {
      if (invoiceAddress == null || companyAddress == null) {
        return invoiceLineInformation;
      } else {
        if (invoiceAddress.getState() == companyAddress.getState()) {
          invoiceLineInformation.put("cgst", price.multiply(tax).divide(Two));
          invoiceLineInformation.put("sgst", price.multiply(tax).divide(Two));
        } else {
          invoiceLineInformation.put("igst", price.multiply(tax));
          System.err.println( ); 
        }
      }
    }
    return invoiceLineInformation;
  }

  @Override
  public Map<String, Object> resetProductInformation(Invoice invoice) throws AxelorException {
    Map<String, Object> productInformation = super.resetProductInformation(invoice);
    productInformation.put("gstRate", null);
    productInformation.put("hsbn", null);
    productInformation.put("igst", null);
    productInformation.put("cgst", null);
    productInformation.put("sgst", null);
    return productInformation;
  }

  @Override
  public Map<String, Object> compute(Invoice invoice, InvoiceLine invoiceLine) throws AxelorException {
    
    Map<String, Object>invoiceLineInformation = new HashMap<String, Object>();
    
    BigDecimal exTaxTotal;
    BigDecimal companyExTaxTotal;
    BigDecimal inTaxTotal;
    BigDecimal companyInTaxTotal;
    BigDecimal priceDiscounted =
        Beans.get(InvoiceLineService.class).computeDiscount(invoiceLine, invoice.getInAti());

    invoiceLineInformation.put("priceDiscounted", priceDiscounted);

    BigDecimal taxRate = BigDecimal.ZERO;
    if (invoiceLine.getTaxLine() != null) {
      taxRate = invoiceLine.getTaxLine().getValue();
      invoiceLineInformation.put("taxRate", taxRate);
      invoiceLineInformation.put("taxCode", invoiceLine.getTaxLine().getTax().getCode());
    }
    
    if (taxRate == BigDecimal.ZERO)
      taxRate = invoiceLine.getGstRate();
    
    if (!invoice.getInAti()) {
      exTaxTotal = InvoiceLineManagement.computeAmount(invoiceLine.getQty(), priceDiscounted);
      inTaxTotal = exTaxTotal.add(exTaxTotal.multiply(taxRate));
    } else {
      inTaxTotal = InvoiceLineManagement.computeAmount(invoiceLine.getQty(), priceDiscounted);
      exTaxTotal = inTaxTotal.divide(taxRate.add(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP);
    }

    companyExTaxTotal = invoiceLineService.getCompanyExTaxTotal(exTaxTotal, invoice);
    companyInTaxTotal = invoiceLineService.getCompanyExTaxTotal(inTaxTotal, invoice);

    invoiceLineInformation.put("exTaxTotal", exTaxTotal);
    invoiceLineInformation.put("inTaxTotal", inTaxTotal);
    invoiceLineInformation.put("companyInTaxTotal", companyInTaxTotal);
    invoiceLineInformation.put("companyExTaxTotal", companyExTaxTotal);
    invoiceLineInformation = this.calculateGst(invoice, invoiceLine, invoiceLineInformation);
    
    return invoiceLineInformation;
  }
}
