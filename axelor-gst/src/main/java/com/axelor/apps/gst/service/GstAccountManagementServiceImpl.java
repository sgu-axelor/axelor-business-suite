package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.AccountManagement;
import com.axelor.apps.account.db.FiscalPosition;
import com.axelor.apps.account.db.Tax;
import com.axelor.apps.account.service.AccountManagementServiceAccountImpl;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.service.app.AppService;
import com.axelor.apps.base.service.tax.FiscalPositionService;
import com.axelor.apps.base.service.tax.TaxService;
import com.axelor.apps.gst.exceptions.IExceptionMessage;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.util.List;

public class GstAccountManagementServiceImpl extends AccountManagementServiceAccountImpl {

  final String GSTCODE = "GST";
  FiscalPositionService fiscalPositionService;

  @Inject
  public GstAccountManagementServiceImpl(
      FiscalPositionService fiscalPositionService, TaxService taxService) {
    super(fiscalPositionService, taxService);
    this.fiscalPositionService = fiscalPositionService;
  }

  @Override
  public AccountManagement getAccountManagement(
      List<AccountManagement> accountManagements, Company company) {
    AccountManagement accountManagement = super.getAccountManagement(accountManagements, company);
    if (Beans.get(AppService.class).isApp("gst")) {
      for (AccountManagement accountManagement1 : accountManagements) {
        if (accountManagement1.getCompany().equals(company)) {
          if (accountManagement1.getSaleTax() != null
              && accountManagement1.getSaleTax().getCode().equals(GSTCODE)) {
            return accountManagement1;
          }
        }
      }
      return null;
    }

    return accountManagement;
  }

  @Override
  public Tax getProductTax(
      Product product, Company company, FiscalPosition fiscalPosition, boolean isPurchase)
      throws AxelorException {

    try {
      return super.getProductTax(product, company, fiscalPosition, isPurchase);
    } catch (AxelorException e) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get(IExceptionMessage.GST_TAX_LINE_ERROR),
          product.getCode(),
          company.getName());
    }
  }
}
