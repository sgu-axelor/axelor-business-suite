package com.axelor.apps.gst.web;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.gst.service.InvoiceService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.ArrayList;

public class InvoiceController {

  public void setInvoiceLines(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Invoice invoice = request.getContext().asType(Invoice.class);
    ArrayList<Integer> ids = (ArrayList<Integer>) request.getContext().get("_ids");

    if (ids != null && !ids.isEmpty() && invoice.getPartner() != null) {
      invoice.setInvoiceLineList(Beans.get(InvoiceService.class).setInvoiceLines(ids, invoice));
    }
    response.setValues(invoice);
  }
}
