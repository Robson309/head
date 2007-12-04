package org.mifos.application.accounts.loan.persistance;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mifos.application.NamedQueryConstants;
import org.mifos.application.accounts.business.AccountActionDateEntity;
import org.mifos.application.accounts.business.AccountBO;
import org.mifos.application.accounts.business.AccountFeesEntity;
import org.mifos.application.accounts.business.AccountPaymentEntity;
import org.mifos.application.accounts.business.AccountTrxnEntity;
import org.mifos.application.accounts.loan.business.LoanBO;
import org.mifos.application.accounts.loan.util.helpers.LoanConstants;
import org.mifos.application.accounts.util.helpers.AccountActionTypes;
import org.mifos.application.accounts.util.helpers.AccountStates;
import org.mifos.application.accounts.util.helpers.AccountTypes;
import org.mifos.application.accounts.util.helpers.PaymentStatus;
import org.mifos.application.productdefinition.business.LoanOfferingBO;
import org.mifos.application.productdefinition.business.LoanOfferingFundEntity;
import org.mifos.framework.components.configuration.business.Configuration;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.hibernate.helper.HibernateUtil;
import org.mifos.framework.persistence.Persistence;
import org.mifos.framework.util.helpers.DateUtils;
import org.mifos.framework.util.helpers.Money;

public class LoanPersistence extends Persistence {

	public Double getFeeAmountAtDisbursement(Integer accountId)
			throws PersistenceException {
		Money amount = new Money();
		HashMap<String, Object> queryParameters = new HashMap<String, Object>();
		queryParameters.put("ACCOUNT_ID", accountId);
		List<AccountFeesEntity> queryResult = executeNamedQuery(
				NamedQueryConstants.GET_FEE_AMOUNT_AT_DISBURSEMENT,
				queryParameters);
		for (AccountFeesEntity entity : queryResult)
			amount = amount.add(entity.getAccountFeeAmount());
		return amount.getAmountDoubleValue();
	}

	public LoanBO findBySystemId(String accountGlobalNum)
			throws PersistenceException {
		Map<String, String> queryParameters = new HashMap<String, String>();
		queryParameters.put("globalAccountNumber", accountGlobalNum);
		Object queryResult = execUniqueResultNamedQuery(
				NamedQueryConstants.FIND_ACCOUNT_BY_SYSTEM_ID, queryParameters);
		return queryResult == null ? null : (LoanBO) queryResult;
	}

	public List<LoanBO> findIndividualLoans(String accountId)
			throws PersistenceException {
		Map<String, String> queryParameters = new HashMap<String, String>();
		queryParameters.put(LoanConstants.LOANACCOUNTID, accountId);
		List<LoanBO> queryResult = executeNamedQuery(
				NamedQueryConstants.FIND_INDIVIDUAL_LOANS, queryParameters);
		return queryResult == null ? null : (List<LoanBO>) queryResult;
	}
		
	public List<Integer> getLoanAccountsInArrearsInGoodStanding(
			Short latenessDays) throws PersistenceException {

		String systemDate = DateUtils.getCurrentDate();
		Date localDate = DateUtils.getLocaleDate(systemDate);
		Calendar currentDate = new GregorianCalendar();
		currentDate.setTime(localDate);
		int year = currentDate.get(Calendar.YEAR);
		int month = currentDate.get(Calendar.MONTH);
		int day = currentDate.get(Calendar.DAY_OF_MONTH);
		currentDate = new GregorianCalendar(year, month, day - latenessDays);
		Date date = new Date(currentDate.getTimeInMillis());

		Map<String, Object> queryParameters = new HashMap<String, Object>();
		queryParameters.put("ACCOUNTTYPE_ID", AccountTypes.LOAN_ACCOUNT
				.getValue());
		queryParameters.put("PAYMENTSTATUS", Short.valueOf(PaymentStatus.UNPAID
				.getValue()));
		queryParameters.put("LOANAPPROVED", Short
				.valueOf(AccountStates.LOANACC_APPROVED));
		queryParameters.put("LOANACTIVEINGOODSTAND", Short
				.valueOf(AccountStates.LOANACC_ACTIVEINGOODSTANDING));
		queryParameters.put("CHECKDATE", date);

		return executeNamedQuery(
				NamedQueryConstants.GET_LOAN_ACOUNTS_IN_ARREARS_IN_GOOD_STANDING,
				queryParameters);
	}

	public List<Integer> getLoanAccountsInArrears(Short latenessDays)
			throws PersistenceException {
		Map<String, Object> queryParameters = new HashMap<String, Object>();

		Calendar currentDate = new GregorianCalendar();
		currentDate.add(Calendar.DAY_OF_MONTH, -latenessDays);

		currentDate = new GregorianCalendar(currentDate.get(Calendar.YEAR),
				currentDate.get(Calendar.MONTH), currentDate
						.get(Calendar.DAY_OF_MONTH), 0, 0, 0);

		queryParameters.put("ACCOUNTTYPE_ID", AccountTypes.LOAN_ACCOUNT
				.getValue());
		queryParameters.put("PAYMENTSTATUS", Short.valueOf(PaymentStatus.UNPAID
				.getValue()));
		queryParameters.put("BADSTANDING", Short
				.valueOf(AccountStates.LOANACC_BADSTANDING));
		queryParameters.put("LOANACTIVEINGOODSTAND", Short
				.valueOf(AccountStates.LOANACC_ACTIVEINGOODSTANDING));

		queryParameters.put("CHECKDATE", currentDate.getTime());

		return executeNamedQuery(
				NamedQueryConstants.GET_LOAN_ACOUNTS_IN_ARREARS,
				queryParameters);
	}

	public LoanBO getAccount(Integer accountId) throws PersistenceException {
		return (LoanBO) getPersistentObject(LoanBO.class, accountId);
	}

	public Short getLastPaymentAction(Integer accountId)
			throws PersistenceException {
		HashMap queryParameters = new HashMap();
		queryParameters.put("accountId", accountId);
		List<AccountPaymentEntity> accountPaymentList = executeNamedQuery(
				NamedQueryConstants.RETRIEVE_MAX_ACCPAYMENT, queryParameters);
		if (accountPaymentList != null && accountPaymentList.size() > 0) {
			AccountPaymentEntity accountPayment = accountPaymentList.get(0);
			Set<AccountTrxnEntity> accountTrxnSet = accountPayment
					.getAccountTrxns();
			for (AccountTrxnEntity accountTrxn : accountTrxnSet) {
				if (accountTrxn.getAccountActionEntity().getId().shortValue() == AccountActionTypes.DISBURSAL
						.getValue()) {
					return accountTrxn.getAccountActionEntity().getId();
				}
			}
		}
		return null;
	}

	public LoanOfferingBO getLoanOffering(Short loanOfferingId, Short localeId)
			throws PersistenceException {
		LoanOfferingBO loanOffering = (LoanOfferingBO) getPersistentObject(
				LoanOfferingBO.class, loanOfferingId);
		if (loanOffering.getLoanOfferingFunds() != null
				&& loanOffering.getLoanOfferingFunds().size() > 0)
			for (LoanOfferingFundEntity loanOfferingFund : loanOffering
					.getLoanOfferingFunds()) {
				loanOfferingFund.getFund().getFundId();
				loanOfferingFund.getFund().getFundName();
			}
		loanOffering.getInterestTypes().setLocaleId(localeId);
		loanOffering.getGracePeriodType().setLocaleId(localeId);
		return loanOffering;
	}

	public List<LoanBO> getSearchResults(String officeId, String personnelId,
			String type, String currentStatus) throws PersistenceException {
		Map<String, Object> queryParameters = new HashMap<String, Object>();
		queryParameters.put("OFFICE_ID", officeId);
		queryParameters.put("PERSONNEL_ID", personnelId);
		queryParameters.put("CURRENT_STATUS", currentStatus);
		return executeNamedQuery(NamedQueryConstants.GET_SEARCH_RESULTS,
				queryParameters);
	}

	public void deleteInstallments(
			Set<AccountActionDateEntity> accountActionDates)
			throws PersistenceException {
		try {
			Session session = HibernateUtil.getSessionTL();
			for (AccountActionDateEntity entity : accountActionDates) {
				session.delete(entity);
			}
		}
		catch (HibernateException he) {
			throw new PersistenceException(he);
		}
	}

	public AccountBO getLoanAccountWithAccountActionsInitialized(
			Integer accountId) throws PersistenceException {
		Map<String, Object> queryParameters = new HashMap<String, Object>();
		queryParameters.put("accountId", accountId);
		List obj = executeNamedQuery(
				"accounts.retrieveLoanAccountWithAccountActions",
				queryParameters);
		Object[] obj1 = (Object[]) obj.get(0);
		return (AccountBO) obj1[0];
	}

	public Money getLastLoanAmountForCustomer(Integer customerId)
			throws PersistenceException {
		Map<String, Object> queryParameters = new HashMap<String, Object>();
		queryParameters.put("customerId", customerId);
		Object obj = execUniqueResultNamedQuery(
				NamedQueryConstants.LAST_LOAN_AMOUNT_CUSTOMER, queryParameters);
		if (null != obj) {
			return (Money) obj;
		}
		return null;
	}

	public List<LoanBO> getLoanAccountsInActiveBadStanding(Short branchId,
			Short loanOfficerId, Short loanProductId)
			throws PersistenceException {
		String activeBadAccountIdQuery = "from org.mifos.application.accounts.loan.business.LoanBO loan where loan.accountState.id = 9";
		StringBuilder queryString = loanQueryString(branchId, loanOfficerId, loanProductId,activeBadAccountIdQuery);
		try {
			Session session = HibernateUtil.getSessionTL();
			Query query = session.createQuery(queryString.toString());
			return query.list();
		}
		catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	
	@SuppressWarnings("cast")
	public List<LoanBO> getLoanAccountsActiveInGoodBadStanding(
			Integer customerId)
			throws PersistenceException {
		try {
			HashMap<String, Object> queryParameters = new HashMap<String, Object>();
			queryParameters.put(LoanConstants.LOANACTIVEINGOODSTAND, AccountStates.LOANACC_ACTIVEINGOODSTANDING);
			queryParameters.put(LoanConstants.CUSTOMER, customerId);
			queryParameters.put(LoanConstants.LOANACTIVEINBADSTAND, AccountStates.LOANACC_BADSTANDING);
			queryParameters.put(LoanConstants.ACCOUNTTYPE_ID, AccountTypes.LOAN_ACCOUNT.getValue());

			return (List<LoanBO>) executeNamedQuery(
					NamedQueryConstants.ACCOUNT_GETALLLOANBYCUSTOMER,
					queryParameters);

		}
		catch (Exception e) {
			throw new PersistenceException(e);
		}
	}
	
	public BigDecimal getTotalOutstandingPrincipalOfLoanAccountsInActiveGoodStanding(
			Short branchId, Short loanOfficerId, Short loanProductId) throws PersistenceException{
		BigDecimal loanBalanceAmount = new BigDecimal(0);
		try {
			Session session = HibernateUtil.getSessionTL();
			Criteria criteria = session.createCriteria(LoanBO.class)
					.setProjection(Projections.sum("loanBalance.amount"))
					.add(Restrictions.eq("accountState.id", (short) 5))
					.add(Restrictions.eq("office.officeId", branchId));
			if (loanOfficerId != (short)-1) {
				criteria.add(Restrictions.eq("personnel.personnelId", loanOfficerId));
			}
			if (loanProductId != (short)-1) {
				criteria.add(Restrictions.eq("loanOffering.prdOfferingId", loanProductId));
			}
			
			List list = criteria.list();
			loanBalanceAmount = (BigDecimal) list.get(0);
				}
		catch (Exception e) {
			throw new PersistenceException(e);
		}
		return loanBalanceAmount;
	}


	public List<LoanBO> getActiveLoansBothInGoodAndBadStandingByLoanOfficer(Short branchId, Short loanOfficerId, Short loanProductId) throws PersistenceException {

		String activeLoansQuery = "from org.mifos.application.accounts.loan.business.LoanBO loan where loan.accountState.id in (5,9)";
		StringBuilder queryString = loanQueryString(branchId, loanOfficerId, loanProductId,activeLoansQuery);
		try {
			Session session = HibernateUtil.getSessionTL();
			Query query = session.createQuery(queryString.toString());
			return query.list();
		}
		catch (Exception e) {
			throw new PersistenceException(e);
		}
	
	}
	private StringBuilder loanQueryString(Short branchId, Short loanOfficerId, Short loanProductId,String goodAccountIdQueryString) {
		
		StringBuilder queryString = new StringBuilder(goodAccountIdQueryString);
		if (loanOfficerId != (short)-1) {
			queryString.append(" and loan.personnel.personnelId = "
					+ loanOfficerId);
		}
		if (loanProductId != (short)-1) {
			queryString.append(" and loan.loanOffering.prdOfferingId = "
					+ loanProductId);
		}
		queryString.append(" and loan.office.officeId = " + branchId);
		return queryString;
	}
}
