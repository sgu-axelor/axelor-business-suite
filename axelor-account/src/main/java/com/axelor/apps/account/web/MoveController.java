/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.account.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.report.IReport;
import com.axelor.apps.account.service.extract.ExtractContextMoveService;
import com.axelor.apps.account.service.move.MoveService;
import com.axelor.apps.base.db.repo.YearRepository;
import com.axelor.apps.base.service.PeriodService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.exception.ResponseMessageType;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class MoveController {

  @Inject protected MoveService moveService;

  @Inject protected MoveRepository moveRepo;

  public void validate(ActionRequest request, ActionResponse response) {

    Move move = request.getContext().asType(Move.class);
    move = moveRepo.find(move.getId());

    try {
      moveService.getMoveValidateService().validate(move);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.ERROR);
    }
  }

  public void getPeriod(ActionRequest request, ActionResponse response) {

    Move move = request.getContext().asType(Move.class);

    try {
      if (move.getDate() != null && move.getCompany() != null) {

        response.setValue(
            "period",
            Beans.get(PeriodService.class)
                .getActivePeriod(move.getDate(), move.getCompany(), YearRepository.TYPE_FISCAL));
      } else {
        response.setValue("period", null);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void generateReverse(ActionRequest request, ActionResponse response) {

    try {
      Context context = request.getContext();

      Move move = context.asType(Move.class);
      move = moveRepo.find(move.getId());

      Map<String, Object> assistantMap =
          Beans.get(ExtractContextMoveService.class)
              .getMapFromMoveWizardGenerateReverseForm(context);

      Move newMove = moveService.generateReverse(move, assistantMap);
      if (newMove != null) {
        response.setView(
            ActionView.define(I18n.get("Account move"))
                .model("com.axelor.apps.account.db.Move")
                .param("forceEdit", "true")
                .context("_showRecord", newMove.getId().toString())
                .map());
        response.setCanClose(true);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  @SuppressWarnings("unchecked")
  public void validateMultipleMoves(ActionRequest request, ActionResponse response) {
    List<Long> moveIds = (List<Long>) request.getContext().get("_ids");
    if (moveIds != null && !moveIds.isEmpty()) {

      List<? extends Move> moveList =
          moveRepo
              .all()
              .filter(
                  "self.id in ?1 AND self.statusSelect NOT IN (?2, ?3)",
                  moveIds,
                  MoveRepository.STATUS_VALIDATED,
                  MoveRepository.STATUS_CANCELED)
              .order("date")
              .fetch();
      if (!moveList.isEmpty()) {
        boolean error = moveService.getMoveValidateService().validateMultiple(moveList);
        if (error) response.setFlash(I18n.get(IExceptionMessage.MOVE_VALIDATION_NOT_OK));
        else {
          response.setFlash(I18n.get(IExceptionMessage.MOVE_VALIDATION_OK));
          response.setReload(true);
        }
      } else response.setFlash(I18n.get(IExceptionMessage.NO_MOVES_SELECTED));
    } else response.setFlash(I18n.get(IExceptionMessage.NO_MOVES_SELECTED));
  }

  // change move status to Archived=true
  public void deleteMove(ActionRequest request, ActionResponse response) throws AxelorException {

    Move move = request.getContext().asType(Move.class);
    move = moveRepo.find(move.getId());

    if (move.getStatusSelect().equals(MoveRepository.STATUS_NEW)) {
      moveRepo.remove(move);
      response.setFlash(I18n.get(IExceptionMessage.MOVE_ARCHIVE_OK));
      response.setView(
          ActionView.define("Moves")
              .model(Move.class.getName())
              .add("grid", "move-grid")
              .add("form", "move-grid")
              .map());
      response.setCanClose(true);
    } else {
      try {
        moveRepo.remove(move);
      } catch (Exception e) {
        TraceBackService.trace(response, e, ResponseMessageType.ERROR);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void deleteMultipleMoves(ActionRequest request, ActionResponse response) {

    List<Long> moveIds = (List<Long>) request.getContext().get("_ids");
    if (!moveIds.isEmpty()) {
      List<? extends Move> moveList =
          moveRepo
              .all()
              .filter(
                  "self.id in ?1 AND self.statusSelect = ?2 AND (self.archived = false or self.archived = null)",
                  moveIds,
                  MoveRepository.STATUS_NEW)
              .fetch();
      if (!moveList.isEmpty()) {
        moveService.getMoveRemoveService().deleteMultiple(moveList);
        response.setFlash(I18n.get(IExceptionMessage.MOVE_ARCHIVE_OK));
        response.setReload(true);
      } else response.setFlash(I18n.get(IExceptionMessage.NO_MOVE_TO_ARCHIVE));
    } else response.setFlash(I18n.get(IExceptionMessage.NO_MOVE_TO_ARCHIVE));
  }

  public void printMove(ActionRequest request, ActionResponse response) throws AxelorException {

    Move move = request.getContext().asType(Move.class);
    move = moveRepo.find(move.getId());

    String moveName = move.getReference().toString();

    String fileLink =
        ReportFactory.createReport(IReport.ACCOUNT_MOVE, moveName + "-${date}")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("moveId", move.getId())
            .generate()
            .getFileLink();

    response.setView(ActionView.define(moveName).add("html", fileLink).map());
  }

  public void showMoveLines(ActionRequest request, ActionResponse response) {

    ActionViewBuilder actionViewBuilder = ActionView.define(I18n.get("Move Lines"));
    actionViewBuilder.model(MoveLine.class.getName());
    actionViewBuilder.add("grid", "move-line-grid");
    actionViewBuilder.add("form", "move-line-form");
    actionViewBuilder.param("search-filters", "move-line-filters");

    if (request.getContext().get("_accountingReportId") != null) {
      Long accountingReportId =
          Long.valueOf(request.getContext().get("_accountingReportId").toString());
      actionViewBuilder.domain("self.move.accountingReport.id = " + accountingReportId);
    }

    response.setView(actionViewBuilder.map());
  }

  public void updateInDayBookMode(ActionRequest request, ActionResponse response) {

    Move move = request.getContext().asType(Move.class);
    move = moveRepo.find(move.getId());

    try {
      if (move.getStatusSelect() == MoveRepository.STATUS_DAYBOOK) {
        moveService.getMoveValidateService().updateInDayBookMode(move);
        response.setReload(true);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void computeTotals(ActionRequest request, ActionResponse response) {
    Move move = request.getContext().asType(Move.class);
    Map<String, Object> values = moveService.computeTotals(move);
    response.setValues(values);
  }

  public void autoTaxLineGenerate(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Move move = request.getContext().asType(Move.class);
    if (move.getMoveLineList() != null
        && !move.getMoveLineList().isEmpty()
        && move.getStatusSelect().equals(MoveRepository.STATUS_NEW)) {
      moveService.getMoveLineService().autoTaxLineGenerate(move);
      response.setValue("moveLineList", move.getMoveLineList());
    }
  }
  
 
}
