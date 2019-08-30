package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.exception.AxelorException;
import java.util.ArrayList;

public interface InvoiceService {

  ArrayList<InvoiceLine> setInvoiceLines(ArrayList<Integer> ids, Invoice invoice)
      throws AxelorException;
}
