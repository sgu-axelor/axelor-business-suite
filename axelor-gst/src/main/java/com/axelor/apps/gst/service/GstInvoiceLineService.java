package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.exception.AxelorException;

public interface GstInvoiceLineService {
  void compute(Invoice invoice, InvoiceLine invoiceLine) throws AxelorException;
}
