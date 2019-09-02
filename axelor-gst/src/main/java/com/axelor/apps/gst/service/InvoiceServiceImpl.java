package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.service.invoice.InvoiceLineServiceImpl;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;

public class InvoiceServiceImpl implements InvoiceService {

  private InvoiceLineServiceImpl invoiceLineServiceImpl;
  private ProductRepository productRepository;
  private GstInvoiceLineServiceImpl gstInvoiceLineServiceImpl;

  @Inject
  public InvoiceServiceImpl(
      ProductRepository productRepository,
      InvoiceLineServiceImpl invoiceLineServiceImpl,
      GstInvoiceLineServiceImpl gstInvoiceLineServiceImpl) {
    this.productRepository = productRepository;
    this.invoiceLineServiceImpl = invoiceLineServiceImpl;
    this.gstInvoiceLineServiceImpl = gstInvoiceLineServiceImpl;
  }

  @Override
  public ArrayList<InvoiceLine> setInvoiceLines(ArrayList<Integer> ids, Invoice invoice)
      throws AxelorException {
    ArrayList<InvoiceLine> invoiceLineList = new ArrayList<InvoiceLine>();
    for (Integer id : ids) {
      Product product = productRepository.find(id.longValue());
      InvoiceLine invoiceLine = new InvoiceLine();
      try {
        invoiceLine.setProduct(product);
        invoiceLine.setQty(BigDecimal.ONE);
        invoiceLine.setProductCode(product.getCode());
        invoiceLine.setProductName(product.getName());
        invoiceLine.setHsbn(product.getHsbn());
        invoiceLine.setGstRate(product.getGstRate());
        invoiceLine.setUnit(product.getUnit());
        invoiceLine.setPrice(product.getSalePrice());
        invoiceLine.setExTaxTotal(invoiceLine.getPrice().multiply(invoiceLine.getQty()));
        invoiceLineList.add(invoiceLine);
        invoiceLine.setTaxLine(invoiceLineServiceImpl.getTaxLine(invoice, invoiceLine, false));
      } catch (Exception e) {
        System.err.println(e.getMessage());
      }
      if (invoiceLine.getTaxLine() != null) {
        invoiceLine.setTaxRate(invoiceLine.getTaxLine().getValue());
      } else {
        invoiceLine.setTaxRate(invoiceLine.getGstRate());
      }
      gstInvoiceLineServiceImpl.compute(invoice, invoiceLine);
    }
    return invoiceLineList;
  }

  /*  public InvoiceLine createInvoiceLines(Invoice invoice, Product product) {
    InvoiceLineGenerator invoiceLineGenerator =
        new InvoiceLineGenerator(
            invoice,
            product,
            product.getName(),
            product.getDescription(),
            BigDecimal.ONE,
            product.getUnit(),
            invoice.getPriority(),
            false,
            false,
            0) {

          @Override
          public List<InvoiceLine> creates() throws AxelorException {
            InvoiceLine invoiceLine = this.createInvoiceLine();
            ArrayList<InvoiceLine> invoicelines = new ArrayList<InvoiceLine>();
            invoicelines.add(invoiceLine);
            return invoicelines;
          }
        };

    try {
      return invoiceLineGenerator.creates().get(0);
    } catch (AxelorException e) {
      e.printStackTrace();
    }
    return null;
  }*/
}
