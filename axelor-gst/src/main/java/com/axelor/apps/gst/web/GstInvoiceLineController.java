package com.axelor.apps.gst.web;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.service.invoice.InvoiceLineService;
import com.axelor.apps.account.web.InvoiceLineController;
import com.axelor.apps.gst.service.GstInvoiceLineService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import java.math.BigDecimal;
import java.util.Map;

public class GstInvoiceLineController extends InvoiceLineController {

  public void compute(ActionRequest request, ActionResponse response) throws AxelorException {

    Context context = request.getContext();

    InvoiceLine invoiceLine = context.asType(InvoiceLine.class);

    if (context.getParent().getContextClass() == InvoiceLine.class) {
      context = request.getContext().getParent();
    }

    Invoice invoice = this.getInvoice(context);
    BigDecimal priceDiscounted =
        Beans.get(InvoiceLineService.class).computeDiscount(invoiceLine, invoice.getInAti());

    if (invoice == null
        || invoiceLine.getPrice() == null
        || invoiceLine.getInTaxPrice() == null
        || invoiceLine.getQty() == null) {
      return;
    }

    response.setAttr(
        "priceDiscounted",
        "hidden",
        priceDiscounted.compareTo(
                invoice.getInAti() ? invoiceLine.getInTaxPrice() : invoiceLine.getPrice())
            == 0);
    Map<String, Object> invoiceLineInformation =
        Beans.get(GstInvoiceLineService.class).compute(invoice, invoiceLine);
    response.setValues(invoiceLineInformation);
  }
}
