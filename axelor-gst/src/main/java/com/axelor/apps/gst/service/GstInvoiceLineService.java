package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.exception.AxelorException;
import java.util.Map;

public interface GstInvoiceLineService {
  Map<String, Object> compute(Invoice invoice, InvoiceLine invoiceLine) throws AxelorException;
}
