package com.axelor.apps.gst.service;

import java.util.List;

import com.axelor.apps.account.db.AccountManagement;
import com.axelor.apps.account.db.FiscalPosition;
import com.axelor.apps.account.db.Tax;
import com.axelor.apps.account.service.AccountManagementServiceAccountImpl;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.service.tax.FiscalPositionService;
import com.axelor.apps.base.service.tax.TaxService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;

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
    if (accountManagements == null || accountManagements.isEmpty()) {
      return null;
    }

    /*    for (Integer i = accountManagements.size()-1; i >=0 ; i--) {
      if (accountManagements.get(i).getCompany().equals(company)) {
        if (accountManagements.get(i).getSaleTax().getCode().equals(GSTCODE)) {
          return accountManagements.get(i);
        }
        if (i == 0) {
          return accountManagements.get(i);
        }
      }
    }*/

    for (AccountManagement accountManagement : accountManagements) {
      if (accountManagement.getCompany().equals(company)) {
        if (accountManagement.getSaleTax().getCode().equals(GSTCODE)) {
          return accountManagement;
        }
      }
    }

    return null;
  }
  
  @Override
  public Tax getProductTax(
      Product product, Company company, FiscalPosition fiscalPosition, boolean isPurchase)
      throws AxelorException {

    Tax generalTax = this.getProductTax(product, company, isPurchase, CONFIG_OBJECT_PRODUCT);

    Tax tax = fiscalPositionService.getTax(fiscalPosition, generalTax);

    if (tax != null) {
      return tax;
    }

    throw new AxelorException(
        TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
        I18n.get(com.axelor.apps.gst.exceptions.IExceptionMessage.ACCOUNT_MANAGEMENT_3),
        product.getCode(),
        company.getName());
  }
}
