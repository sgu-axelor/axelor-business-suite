package com.axelor.apps.gst.service;

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
import com.axelor.apps.base.service.app.AppService;
import com.axelor.apps.businessproject.service.InvoiceLineProjectServiceImpl;
import com.axelor.apps.gst.exceptions.IExceptionMessage;
import com.axelor.apps.purchase.service.PurchaseProductService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.Map;

public class GstInvoiceLineServiceImpl extends InvoiceLineProjectServiceImpl
    implements GstInvoiceLineService {

  InvoiceLineService invoiceLineService;
  private AppService appService;

  @Inject
  public GstInvoiceLineServiceImpl(
      CurrencyService currencyService,
      PriceListService priceListService,
      AppAccountService appAccountService,
      AnalyticMoveLineService analyticMoveLineService,
      AccountManagementAccountService accountManagementAccountService,
      PurchaseProductService purchaseProductService,
      InvoiceLineService invoiceLineService,
      AppService appService) {
    super(
        currencyService,
        priceListService,
        appAccountService,
        analyticMoveLineService,
        accountManagementAccountService,
        purchaseProductService);
    this.invoiceLineService = invoiceLineService;
    this.appService = appService;
  }

  @Override
  public Map<String, Object> fillProductInformation(Invoice invoice, InvoiceLine invoiceLine)
      throws AxelorException {
    Map<String, Object> productInformation = super.fillProductInformation(invoice, invoiceLine);
    if (appService.isApp("gst")) {
      productInformation.put("gstRate", invoiceLine.getProduct().getGstRate());
      productInformation.put("hsbn", invoiceLine.getProduct().getHsbn());
      if (productInformation.get("taxRate") == null && productInformation.get("gstRate") != null) {
        productInformation.put("taxRate", productInformation.get("gstRate"));
      }
    }
    return productInformation;
  }

  public void calculateGst(Invoice invoice, InvoiceLine invoiceLine) throws AxelorException {

    Address invoiceAddress = invoice.getAddress();
    Address companyAddress = invoice.getCompany().getAddress();
    BigDecimal price = invoiceLine.getExTaxTotal();
    BigDecimal tax = invoiceLine.getTaxRate();
    final BigDecimal Two = new BigDecimal("2");

    invoiceLine.setIgst(BigDecimal.ZERO);
    invoiceLine.setCgst(BigDecimal.ZERO);
    invoiceLine.setSgst(BigDecimal.ZERO);
    if (tax != null) {
      if (invoiceAddress == null || companyAddress == null) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.GST_ADDRESS_MESSAGE_1));
      }
      if (invoiceAddress.getState() == null || companyAddress.getState() == null) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.GST_ADDRESS_MESSAGE_2));
      }
      if (invoiceAddress.getState() == companyAddress.getState()) {
        invoiceLine.setCgst(price.multiply(tax).divide(Two, 2, BigDecimal.ROUND_HALF_UP));
        invoiceLine.setSgst(invoiceLine.getCgst());
      } else {
        invoiceLine.setIgst(price.multiply(tax).setScale(2, BigDecimal.ROUND_HALF_UP));
      }
    }
  }

  @Override
  public Map<String, Object> resetProductInformation(Invoice invoice) throws AxelorException {
    Map<String, Object> productInformation = super.resetProductInformation(invoice);
    if (appService.isApp("gst")) {
      productInformation.put("gstRate", null);
      productInformation.put("hsbn", null);
      productInformation.put("igst", null);
      productInformation.put("cgst", null);
      productInformation.put("sgst", null);
    }
    return productInformation;
  }

  public void compute(Invoice invoice, InvoiceLine invoiceLine) throws AxelorException {

    BigDecimal exTaxTotal;
    BigDecimal companyExTaxTotal;
    BigDecimal inTaxTotal;
    BigDecimal companyInTaxTotal;
    BigDecimal priceDiscounted =
        Beans.get(InvoiceLineService.class).computeDiscount(invoiceLine, invoice.getInAti());

    invoiceLine.setPriceDiscounted(priceDiscounted);

    BigDecimal taxRate = BigDecimal.ZERO;
    if (invoiceLine.getTaxLine() != null) {
      taxRate = invoiceLine.getTaxLine().getValue();
      invoiceLine.setTaxCode(invoiceLine.getTaxLine().getTax().getCode());
    } else if (invoiceLine.getGstRate().compareTo(BigDecimal.ZERO) > 0) {
      taxRate = invoiceLine.getGstRate();
    }
    invoiceLine.setTaxRate(taxRate);

    if (!invoice.getInAti()) {
      exTaxTotal =
          InvoiceLineManagement.computeAmount(invoiceLine.getQty(), priceDiscounted)
              .setScale(2, BigDecimal.ROUND_HALF_UP);
      inTaxTotal =
          exTaxTotal.add(exTaxTotal.multiply(taxRate)).setScale(2, BigDecimal.ROUND_HALF_UP);
    } else {
      inTaxTotal = InvoiceLineManagement.computeAmount(invoiceLine.getQty(), priceDiscounted);
      exTaxTotal = inTaxTotal.divide(taxRate.add(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP);
    }

    companyExTaxTotal = invoiceLineService.getCompanyExTaxTotal(exTaxTotal, invoice);
    companyInTaxTotal = invoiceLineService.getCompanyExTaxTotal(inTaxTotal, invoice);

    invoiceLine.setExTaxTotal(exTaxTotal);
    invoiceLine.setInTaxTotal(inTaxTotal);
    invoiceLine.setCompanyInTaxTotal(companyInTaxTotal);
    invoiceLine.setCompanyExTaxTotal(companyExTaxTotal);
    this.calculateGst(invoice, invoiceLine);
  }
}
