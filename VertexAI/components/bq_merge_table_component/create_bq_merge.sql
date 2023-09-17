
MERGE CHURN.CHURN_HISTORY_RAW H
USING CHURN.CHURN_CURRENT_RAW_VIEW  C
ON H.Customer_ID = C.Customer_ID
WHEN MATCHED THEN
  UPDATE SET H.last_modified_date = CURRENT_DATE,H.srvc_prov_state_cd_ab_ind_current_month = C.srvc_prov_state_cd_ab_ind_current_month
WHEN NOT MATCHED THEN
  INSERT (Customer_ID, churn,rec_create_date) VALUES(Customer_ID, churn,rec_create_date);
