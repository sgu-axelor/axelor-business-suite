package com.axelor.apps.gst.web;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppSettings;
import com.axelor.apps.ReportFactory;

import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.gst.report.IReport;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Joiner;

public class ProductController {
  
  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  public void showProducts(ActionRequest request, ActionResponse response ) throws AxelorException {
    ArrayList<Integer> lstSelectedProduct = (ArrayList<Integer>) request.getContext().get("_ids");
    
    User user = Beans.get(UserService.class).getUser();
    String productIds = "";
    
    if (lstSelectedProduct != null) {
      productIds = Joiner.on(",").join(lstSelectedProduct);
    }

    String name = I18n.get("Products");
    
    String fileLink =
        ReportFactory.createReport(IReport.ProductsReport, name + "-${date}")
            .addParam("UserId", user.getId())
            .addParam("ProductIds", productIds)
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .generate()
            .getFileLink();

    logger.debug("Printing " + name);

    response.setView(ActionView.define(name).add("html", fileLink).map());
  }
  
}

