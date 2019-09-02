package com.axelor.apps.gst.service.move;

import com.axelor.apps.account.db.Account;
import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.service.AccountManagementAccountService;
import com.axelor.apps.account.service.AnalyticMoveLineService;
import com.axelor.apps.account.service.FiscalPositionAccountService;
import com.axelor.apps.account.service.TaxAccountService;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.move.MoveLineService;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.service.CurrencyService;
import com.axelor.apps.base.service.app.AppService;
import com.axelor.apps.base.service.config.CompanyConfigService;
import com.axelor.apps.gst.exceptions.IExceptionMessage;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GstMoveLineService extends MoveLineService {

  @Inject
  public GstMoveLineService(
      AccountManagementAccountService accountManagementService,
      TaxAccountService taxAccountService,
      FiscalPositionAccountService fiscalPositionAccountService,
      AppAccountService appAccountService,
      AnalyticMoveLineService analyticMoveLineService,
      CurrencyService currencyService,
      CompanyConfigService companyConfigService) {
    super(
        accountManagementService,
        taxAccountService,
        fiscalPositionAccountService,
        appAccountService,
        analyticMoveLineService,
        currencyService,
        companyConfigService);
  }

  public List<MoveLine> createMoveLines(
      Invoice invoice,
      Move move,
      Company company,
      Partner partner,
      Account partnerAccount,
      boolean consolidate,
      boolean isPurchase,
      boolean isDebitCustomer)
      throws AxelorException {

    List<MoveLine> moveLines = new ArrayList<MoveLine>();
    moveLines =
        super.createMoveLines(
            invoice,
            move,
            company,
            partner,
            partnerAccount,
            consolidate,
            isPurchase,
            isDebitCustomer);

    if (Beans.get(AppService.class).isApp("gst")) {
      for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
        if (invoiceLine.getGstRate().compareTo(BigDecimal.ZERO) == 1
            && invoiceLine.getTaxLine() == null) {
          BigDecimal companyTaxTotal = invoiceLine.getCompanyExTaxTotal();

          if (companyTaxTotal.compareTo(BigDecimal.ZERO) != 0) {
            Account account = invoiceLine.getAccount();
            MoveLine moveLine;
            if (account == null) {
              throw new AxelorException(
                  move,
                  TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
                  I18n.get(IExceptionMessage.GST_TAX_ACCOUNT_ERROR_1),
                  invoiceLine.getProductName());
            }
            moveLine =
                super.createMoveLine(
                    move,
                    partner,
                    account,
                    BigDecimal.ZERO,
                    invoiceLine.getTaxRate().multiply(invoiceLine.getCompanyExTaxTotal()),
                    invoiceLine.getTaxRate().multiply(invoiceLine.getCompanyExTaxTotal()),
                    !isDebitCustomer,
                    invoice.getInvoiceDate(),
                    null,
                    invoice.getOriginDate(),
                    moveLines.size() + 1,
                    invoice.getInvoiceId(),
                    null);
            moveLine.setTaxLine(null);
            moveLine.setTaxRate(invoiceLine.getGstRate());
            moveLines.add(moveLine);
          }
        }
      }
    }
    return moveLines;
  }
}
