package com.axelor.apps.gst.service;

import java.util.Map;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.exception.AxelorException;

public interface GstInvoiceLineService {
  Map<String, Object> compute(Invoice invoice, InvoiceLine invoiceLine) throws AxelorException;
}
