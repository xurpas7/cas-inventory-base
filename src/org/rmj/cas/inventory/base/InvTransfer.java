/**
 * Inventory Transfer BASE
 * @author Michael Torres Cuison
 * @since 2018.10.06
 */
package org.rmj.cas.inventory.base;

import com.mysql.jdbc.Connection;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.cas.inventory.base.views.SubUnitController;
import org.rmj.cas.inventory.others.pojo.UnitInvTransferDetailOthers;
import org.rmj.cas.inventory.pojo.UnitInvMaster;
import org.rmj.cas.inventory.pojo.UnitInvTransferDetail;
import org.rmj.cas.inventory.pojo.UnitInvTransferMaster;
import org.rmj.cas.parameter.agent.XMBranch;
import org.rmj.appdriver.agentfx.callback.IMasterDetail;

public class InvTransfer{
    public InvTransfer(GRider foGRider, String fsBranchCD, boolean fbWithParent){
        this.poGRider = foGRider;
        
        if (foGRider != null){
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;
            
            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
        }
    }
    
    public boolean BrowseRecord(String fsValue, boolean fbByCode){
        String lsHeader = "Transfer No»Destination»Date";
        String lsColName = "sTransNox»sBranchNm»dTransact";
        String lsColCrit = "a.sTransNox»b.sBranchNm»a.dTransact";
        String lsSQL = MiscUtil.addCondition(getSQ_InvTransfer(), "a.sBranchCd = " + SQLUtil.toSQL(psBranchCd));
        
        JSONObject loJSON = showFXDialog.jsonSearch(poGRider, 
                                                    lsSQL, 
                                                    fsValue, 
                                                    lsHeader, 
                                                    lsColName, 
                                                    lsColCrit, 
                                                    fbByCode ? 0 : 1);
        
        if(loJSON == null)
            return false;
        else
            return openTransaction((String) loJSON.get("sTransNox"));
    }
    
    public boolean BrowseAcceptance(String fsValue, boolean fbByCode){
        String lsHeader = "Transfer No»Source»Date";
        String lsColName = "sTransNox»sBranchNm»dTransact";
        String lsColCrit = "a.sTransNox»c.sBranchNm»a.dTransact";
        String lsSQL = MiscUtil.addCondition(getSQ_InvTransfer(), 
                                                "a.sDestinat = " + SQLUtil.toSQL(poGRider.getBranchCode()));
        
        JSONObject loJSON = showFXDialog.jsonSearch(poGRider, 
                                                    lsSQL, 
                                                    fsValue, 
                                                    lsHeader, 
                                                    lsColName, 
                                                    lsColCrit, 
                                                    fbByCode ? 0 : 1);
        
        if(loJSON == null)
            return false;
        else
            return openTransaction((String) loJSON.get("sTransNox"));
    }
    
    public boolean addDetail() {
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvTransferDetail());
            paDetailOthers.add(new UnitInvTransferDetailOthers());
        }
        else{
            if (!paDetail.get(ItemCount()-1).getStockIDx().equals("") &&
                    paDetail.get(ItemCount() -1).getQuantity() != 0){
                paDetail.add(new UnitInvTransferDetail());
                paDetail.get(ItemCount()-1).setOrderNox(paDetail.get(ItemCount()-2).getOrderNox());
                
                paDetailOthers.add(new UnitInvTransferDetailOthers());
            }
                
        }
        return true;
    }

    public boolean deleteDetail(int fnRow) {
        paDetail.remove(fnRow);
        paDetailOthers.remove(fnRow);
        poData.setTranTotl(computeTotal());
        
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvTransferDetail());
            paDetailOthers.add(new UnitInvTransferDetailOthers());
        }            
        
        return true;
    }
    
    public void setDetail(int fnRow, int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poDetail.getColumn("sTransNox") ||
                fnCol == poDetail.getColumn("nEntryNox") ||
                fnCol == poDetail.getColumn("dModified"))){

                if (fnCol == poDetail.getColumn("nQuantity")){
                    if (foData instanceof Integer){
                        if ((int) foData > (int) paDetailOthers.get(fnRow).getValue("nQtyOnHnd"))
                            paDetail.get(fnRow).setValue(fnCol, (int) paDetailOthers.get(fnRow).getValue("nQtyOnHnd"));
                        else
                            paDetail.get(fnRow).setValue(fnCol, foData);
                        
                        addDetail();
                    }else paDetail.get(fnRow).setValue(fnCol, 0);
                } else if (fnCol == poDetail.getColumn("nInvCostx")){
                    if (foData instanceof Number){
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    }else paDetail.get(fnRow).setValue(fnCol, 0.00);
                } else paDetail.get(fnRow).setValue(fnCol, foData);
                
                DetailRetreived(fnCol);
                
                poData.setTranTotl(computeTotal());
                MasterRetreived(12);
            }
        }
    }

    public void setDetail(int fnRow, String fsCol, Object foData) {
        setDetail(fnRow, poDetail.getColumn(fsCol), foData);
    }
    
    public Object getDetailOthers(int fnRow, String fsCol){
        switch(fsCol){
            case "sStockIDx":
            case "nQtyOnHnd":
            case "xQtyOnHnd":
            case "nResvOrdr":
            case "nBackOrdr":
            case "nReorderx":
            case "nLedgerNo":
            case "sBarCodex":
            case "sDescript":
            case "sOrigCode":
            case "sOrigDesc":
            case "sOrderNox":
                return paDetailOthers.get(fnRow).getValue(fsCol);
            default:
                return null;
        }
    }
    
    public Object getDetail(int fnRow, int fnCol) {
        if(pnEditMode == EditMode.UNKNOWN)
         return null;
      else{
         return paDetail.get(fnRow).getValue(fnCol);
      }
    }

    public Object getDetail(int fnRow, String fsCol) {
        return getDetail(fnRow, poDetail.getColumn(fsCol));
    }

    public boolean newTransaction() {
        Connection loConn = null;
        loConn = setConnection();       
        
        poData = new UnitInvTransferMaster();
        poData.setTransNox(MiscUtil.getNextCode(poData.getTable(), "sTransNox", true, loConn, psBranchCd));
        poData.setTransact(poGRider.getServerDate());
        
        paDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //detail other info storage
        addDetail();
        
        pnEditMode = EditMode.ADDNEW;
        return true;
    }
    
    private double computeTotal(){
        double lnTranTotal = 0;
        for (int lnCtr = 0; lnCtr <= ItemCount()-1; lnCtr ++){
            lnTranTotal += ((int) getDetail(lnCtr, "nQuantity") * Double.valueOf(getDetail(lnCtr, "nInvCostx").toString()));
        }
        
        //add the freight charge to total order
        lnTranTotal += Double.valueOf(poData.getFreightx().toString());
        //less the discounts
        lnTranTotal = lnTranTotal - (lnTranTotal * Double.valueOf(poData.getDiscount().toString()));
        return lnTranTotal;
    }
    
    private boolean isInventoryOK(String fsValue){
        int lnMasRow = poData.getEntryNox();
        
        String lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsValue));
        
        try {
            ResultSet loRS = poGRider.executeQuery(lsSQL);
        
            if (MiscUtil.RecordCount(loRS) != lnMasRow){
                lsSQL = MiscUtil.makeSelect(new UnitInvTransferDetail());
                lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));

                loRS = poGRider.executeQuery(lsSQL);
                
                ResultSet loRSx;
                InvMaster loInvMaster = new InvMaster(poGRider, psBranchCd, false);
                
                while (loRS.next()){
                    lsSQL = MiscUtil.makeSelect(new UnitInvMaster());
                    lsSQL = MiscUtil.addCondition(lsSQL, "sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx")) +
                                                            " AND sBranchCD = " + SQLUtil.toSQL(psBranchCd));
                    
                    loRSx = poGRider.executeQuery(lsSQL);
                    if (!loRSx.next()){
                        if (loInvMaster.SearchInventory(loRS.getString("sStockIDx"), false, true)){
                            loInvMaster.NewRecord();
                            if (!loInvMaster.SaveRecord()) {
                                System.err.println(loInvMaster.getMessage());
                                return false;
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return false;
        }

        return true;
    }
    
    public boolean openTransaction(String fsTransNox){
        poData = loadTransaction(fsTransNox);
        
        if (poData != null){ 
            paDetail = loadTransactionDetail(fsTransNox);
        
            if (poData.getEntryNox() != paDetail.size()){
                setMessage("Transaction discrepancy detected... \n" +
                            "Detail count is not equal to the entry number...");
                return false;
            }
        } else{
            setMessage("Unable to load transaction.");
            return false;
        } 
        
        pnEditMode = EditMode.READY;
        return true;
    }

    public UnitInvTransferMaster loadTransaction(String fsTransNox) {
        UnitInvTransferMaster loObject = new UnitInvTransferMaster();
        
        Connection loConn = null;
        loConn = setConnection();   
        
        String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            if (!loRS.next()){
                setMessage("No Record Found");
            }else{
                //load each column to the entity
                for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                    loObject.setValue(lnCol, loRS.getObject(lnCol));
                }
            }              
        } catch (SQLException ex) {
            setErrMsg(ex.getMessage());
        } finally{
            MiscUtil.close(loRS);
            if (!pbWithParent) MiscUtil.close(loConn);
        }
        
        return loObject;
    }
    
    private ArrayList<UnitInvTransferDetail> loadTransactionDetail(String fsTransNox){
        UnitInvTransferDetail loOcc = null;
        UnitInvTransferDetailOthers loOth = null;
        Connection loConn = null;
        loConn = setConnection();
        
        ArrayList<UnitInvTransferDetail> loDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //reset detail others
        
        //2019.05.23
        //  Check first if the transferred items are in the destination's inventory
        if (!isInventoryOK(fsTransNox)) return null;
        
        String lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        
        try {
            ResultSet loRS = poGRider.executeQuery(lsSQL);  
            
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr ++){
                loRS.absolute(lnCtr);

                //load detail
                loOcc = new UnitInvTransferDetail();
                loOcc.setValue("sTransNox", loRS.getString("sTransNox"));        
                loOcc.setValue("nEntryNox", loRS.getInt("nEntryNox"));
                loOcc.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOcc.setValue("sOrigIDxx", loRS.getString("sOrigIDxx"));
                loOcc.setValue("sOrderNox", loRS.getString("sOrderNox"));
                loOcc.setValue("nQuantity", loRS.getInt("nQuantity"));
                loOcc.setValue("nInvCostx", loRS.getDouble("nInvCostx"));
                loOcc.setValue("sRecvIDxx", loRS.getString("sRecvIDxx"));
                loOcc.setValue("nReceived", loRS.getInt("nReceived"));
                loOcc.setValue("sNotesxxx", loRS.getString("sNotesxxx"));
                loOcc.setValue("dModified", loRS.getDate("dModified"));
                loDetail.add(loOcc);
                
                //load other info
                loOth = new UnitInvTransferDetailOthers();
                loOth.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOth.setValue("sBarCodex", loRS.getString("sBarCodex"));
                loOth.setValue("sDescript", loRS.getString("sDescript"));
                loOth.setValue("nQtyOnHnd", loRS.getInt("nQtyOnHnd"));
                loOth.setValue("xQtyOnHnd", loRS.getInt("xQtyOnHnd"));
                loOth.setValue("nResvOrdr", loRS.getInt("nResvOrdr"));
                loOth.setValue("nBackOrdr", loRS.getInt("nBackOrdr"));
                loOth.setValue("nReorderx", 0);
                loOth.setValue("nLedgerNo", loRS.getInt("nLedgerNo"));
                loOth.setValue("sOrigCode", loRS.getString("xBarCodex"));
                paDetailOthers.add(loOth);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }        
        
        return loDetail;
    }
    
    private boolean saveInvTrans(){
        String lsSQL = "";
        ResultSet loRS = null;
        int lnCtr;
        
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        
        /*---------------------------------------------------------------------------------
         *   Credit from mother unit
         *---------------------------------------------------------------------------------*/
        loInvTrans.InitTransaction();
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (!paDetailOthers.get(lnCtr).getValue("sParentID").equals("")){
                lsSQL = "SELECT" +
                            "  nQtyOnHnd" +
                            ", nResvOrdr" +
                            ", nBackOrdr" +
                            ", nLedgerNo" +
                        " FROM Inv_Master" + 
                        " WHERE sStockIDx = " + SQLUtil.toSQL(paDetailOthers.get(lnCtr).getValue("sParentID")) + 
                            " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

                loRS = poGRider.executeQuery(lsSQL);
                
                loInvTrans.setDetail(lnCtr, "sStockIDx", paDetailOthers.get(lnCtr).getValue("sParentID"));
                loInvTrans.setDetail(lnCtr, "nQuantity", paDetailOthers.get(lnCtr).getValue("xParntQty"));
                
                if (MiscUtil.RecordCount(loRS) == 0){
                    loInvTrans.setDetail(lnCtr, "nQtyOnHnd", 0);
                    loInvTrans.setDetail(lnCtr, "nResvOrdr", 0);
                    loInvTrans.setDetail(lnCtr, "nBackOrdr", 0);
                } else{
                    try {
                        loRS.first();
                        loInvTrans.setDetail(lnCtr, "nQtyOnHnd", loRS.getInt("nQtyOnHnd"));
                        loInvTrans.setDetail(lnCtr, "nResvOrdr", loRS.getInt("nResvOrdr"));
                        loInvTrans.setDetail(lnCtr, "nBackOrdr", loRS.getInt("nBackOrdr"));
                        loInvTrans.setDetail(lnCtr, "nLedgerNo", loRS.getInt("nLedgerNo"));
                    } catch (SQLException e) {
                        setMessage("Please inform MIS Department.");
                        setErrMsg(e.getMessage());
                        return false;
                    }
                }
                
                if (!loInvTrans.CreditMemo(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)){
                    setMessage(loInvTrans.getMessage());
                    setErrMsg(loInvTrans.getErrMsg());
                    return false;
                }
            }
        }
        
        
        /*---------------------------------------------------------------------------------
         *   Debit to child unit
         *---------------------------------------------------------------------------------*/
        loInvTrans.InitTransaction();
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (!paDetailOthers.get(lnCtr).getValue("sParentID").equals("")){
                lsSQL = "SELECT" +
                            "  nQtyOnHnd" +
                            ", nResvOrdr" +
                            ", nBackOrdr" +
                            ", nLedgerNo" +
                        " FROM Inv_Master" + 
                        " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                            " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

                loRS = poGRider.executeQuery(lsSQL);
                
                loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
                loInvTrans.setDetail(lnCtr, "nQuantity", paDetailOthers.get(lnCtr).getValue("xQuantity"));
                
                if (MiscUtil.RecordCount(loRS) == 0){
                    loInvTrans.setDetail(lnCtr, "nQtyOnHnd", 0);
                    loInvTrans.setDetail(lnCtr, "nResvOrdr", 0);
                    loInvTrans.setDetail(lnCtr, "nBackOrdr", 0);
                } else{
                    try {
                        loRS.first();
                        loInvTrans.setDetail(lnCtr, "nQtyOnHnd", loRS.getInt("nQtyOnHnd"));
                        loInvTrans.setDetail(lnCtr, "nResvOrdr", loRS.getInt("nResvOrdr"));
                        loInvTrans.setDetail(lnCtr, "nBackOrdr", loRS.getInt("nBackOrdr"));
                        loInvTrans.setDetail(lnCtr, "nLedgerNo", loRS.getInt("nLedgerNo"));
                    } catch (SQLException e) {
                        setMessage("Please inform MIS Department.");
                        setErrMsg(e.getMessage());
                        return false;
                    }
                }
                
                if (!loInvTrans.DebitMemo(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)){
                    setMessage(loInvTrans.getMessage());
                    setErrMsg(loInvTrans.getErrMsg());
                    return false;
                }
            }
        }
        
        /*---------------------------------------------------------------------------------
         *   Save inventory trans of the items
         *---------------------------------------------------------------------------------*/
        loInvTrans.InitTransaction();
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            
            lsSQL = "SELECT" +
                        "  nQtyOnHnd" +
                        ", nResvOrdr" +
                        ", nBackOrdr" +
                        ", nLedgerNo" +
                    " FROM Inv_Master" + 
                    " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

            loRS = poGRider.executeQuery(lsSQL);
            
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "sReplacID", paDetail.get(lnCtr).getOrigIDxx());
            loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getQuantity());
                
            if (MiscUtil.RecordCount(loRS) == 0){
                loInvTrans.setDetail(lnCtr, "nQtyOnHnd", 0);
                loInvTrans.setDetail(lnCtr, "nResvOrdr", 0);
                loInvTrans.setDetail(lnCtr, "nBackOrdr", 0);
            } else{
                try {
                    loRS.first();
                    loInvTrans.setDetail(lnCtr, "nQtyOnHnd", loRS.getInt("nQtyOnHnd"));
                    loInvTrans.setDetail(lnCtr, "nResvOrdr", loRS.getInt("nResvOrdr"));
                    loInvTrans.setDetail(lnCtr, "nBackOrdr", loRS.getInt("nBackOrdr"));
                    loInvTrans.setDetail(lnCtr, "nLedgerNo", loRS.getInt("nLedgerNo"));
                } catch (SQLException e) {
                    setMessage("Please inform MIS Department.");
                    setErrMsg(e.getMessage());
                    return false;
                }
            }
        }
        
        if (!loInvTrans.Delivery(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        
        //TODO
            //update branch order info
    
        return true;
    }
    
    private boolean unsaveInvTrans(){
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQtyOnHnd", paDetailOthers.get(lnCtr).getValue("nQtyOnHnd"));
            loInvTrans.setDetail(lnCtr, "nResvOrdr", paDetailOthers.get(lnCtr).getValue("nResvOrdr"));
            loInvTrans.setDetail(lnCtr, "nBackOrdr", paDetailOthers.get(lnCtr).getValue("nBackOrdr"));
            loInvTrans.setDetail(lnCtr, "nLedgerNo", paDetailOthers.get(lnCtr).getValue("nLedgerNo"));
        }
        
        if (!loInvTrans.Delivery(poData.getTransNox(), poGRider.getServerDate(), EditMode.DELETE)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        
        //TODO
            //update branch order info
    
        return true;
    }
    
    private boolean unpostInvTrans(){
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQtyOnHnd", paDetailOthers.get(lnCtr).getValue("nQtyOnHnd"));
            loInvTrans.setDetail(lnCtr, "nResvOrdr", paDetailOthers.get(lnCtr).getValue("nResvOrdr"));
            loInvTrans.setDetail(lnCtr, "nBackOrdr", paDetailOthers.get(lnCtr).getValue("nBackOrdr"));
            loInvTrans.setDetail(lnCtr, "nLedgerNo", paDetailOthers.get(lnCtr).getValue("nLedgerNo"));
        }
        
        if (!loInvTrans.AcceptDelivery(poData.getTransNox(), poGRider.getServerDate(), EditMode.DELETE)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        
        //TODO
            //update branch order info
    
        return true;
    }
    
    private boolean postInvTrans(){
        String lsSQL = "";
        ResultSet loRS = null;
        int lnCtr;
        
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
              
        /*---------------------------------------------------------------------------------
         *   Save inventory trans of the items
         *---------------------------------------------------------------------------------*/
        loInvTrans.InitTransaction();
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            
            lsSQL = "SELECT" +
                        "  nQtyOnHnd" +
                        ", nResvOrdr" +
                        ", nBackOrdr" +
                        ", nLedgerNo" +
                    " FROM Inv_Master" + 
                    " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

            loRS = poGRider.executeQuery(lsSQL);
            
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "sReplacID", paDetail.get(lnCtr).getOrigIDxx());
            loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getQuantity());
                
            if (MiscUtil.RecordCount(loRS) == 0){
                loInvTrans.setDetail(lnCtr, "nQtyOnHnd", 0);
                loInvTrans.setDetail(lnCtr, "nResvOrdr", 0);
                loInvTrans.setDetail(lnCtr, "nBackOrdr", 0);
            } else{
                try {
                    loRS.first();
                    loInvTrans.setDetail(lnCtr, "nQtyOnHnd", loRS.getInt("nQtyOnHnd"));
                    loInvTrans.setDetail(lnCtr, "nResvOrdr", loRS.getInt("nResvOrdr"));
                    loInvTrans.setDetail(lnCtr, "nBackOrdr", loRS.getInt("nBackOrdr"));
                    loInvTrans.setDetail(lnCtr, "nLedgerNo", loRS.getInt("nLedgerNo"));
                } catch (SQLException e) {
                    setMessage("Please inform MIS Department.");
                    setErrMsg(e.getMessage());
                    return false;
                }
            }
        }
        
        if (!loInvTrans.AcceptDelivery(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        
        //TODO
            //update branch order info
    
        return true;
    }

    public boolean saveTransaction() {
        String lsSQL = "";
        boolean lbUpdate = false;
        
        UnitInvTransferMaster loOldEnt = null;
        UnitInvTransferMaster loNewEnt = null;
        UnitInvTransferMaster loResult = null;
        
        // Check for the value of foEntity
        if (!(poData instanceof UnitInvTransferMaster)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return false;
        }
        
        // Typecast the Entity to this object
        loNewEnt = (UnitInvTransferMaster) poData;
                
        if (loNewEnt.getDestinat()== null || loNewEnt.getDestinat().equals("")){
            setMessage("Invalid destination detected.");
            return false;
        }       
               
        if (!pbWithParent) poGRider.beginTrans();
        
        poData.setTranTotl(computeTotal());
        
        //delete empty detail
        if (paDetail.get(ItemCount()-1).getStockIDx().equals("")) deleteDetail(ItemCount()-1);
        
        // Generate the SQL Statement
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();

            String lsTransNox = MiscUtil.getNextCode(loNewEnt.getTable(), "sTransNox", true, loConn, psBranchCd);

            loNewEnt.setTransNox(lsTransNox);
            loNewEnt.setBranchCd(poGRider.getBranchCode());            
            loNewEnt.setEntryNox(ItemCount());
            loNewEnt.setModified(psUserIDxx);
            loNewEnt.setDateModified(poGRider.getServerDate());
           
            if (!pbWithParent) MiscUtil.close(loConn);

            lbUpdate = saveDetail(loNewEnt.getTransNox());
            if (!lbUpdate) lsSQL = "";
            else lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
        }else{
            //Load previous transaction
            loOldEnt = loadTransaction(poData.getTransNox());

            loNewEnt.setEntryNox(ItemCount());
            loNewEnt.setDateModified(poGRider.getServerDate());
            
            lbUpdate = saveDetail(loNewEnt.getTransNox());
            if (!lbUpdate) lsSQL = "";            
            else lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, (GEntity) loOldEnt, "sTransNox = " + SQLUtil.toSQL(loNewEnt.getValue(1)));
        }
                
        if (!lsSQL.equals("") && getErrMsg().isEmpty()){
            if(poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0){
                if(!poGRider.getErrMsg().isEmpty())
                    setErrMsg(poGRider.getErrMsg());
                else 
                    setMessage("No record updated");
            }
            //lbUpdate = saveInvTrans(); //save inventory legder
        }

        if (!pbWithParent) {
            if (!getErrMsg().isEmpty()){
                poGRider.rollbackTrans();
            } else poGRider.commitTrans();
        }        
        
        return lbUpdate;
    }
    
    private boolean saveDetail(String fsTransNox){
        setMessage("");
        if (paDetail.isEmpty()){
            setMessage("Unable to save empty detail transaction.");
            return false;
        } 
        else if (paDetail.get(0).getStockIDx().equals("") ||
                paDetail.get(0).getQuantity() == 0){
            setMessage("Detail might not have item or zero quantity.");
            return false;
        }
        
        int lnCtr;
        String lsSQL;
        UnitInvTransferDetail loNewEnt = null;
        
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();  
            
            for (lnCtr = 0; lnCtr <= paDetail.size() -1; lnCtr++){
                loNewEnt = paDetail.get(lnCtr);
                
                if (!loNewEnt.getStockIDx().equals("")){
                    if (loNewEnt.getQuantity() == 0){
                       setMessage("Detail might not have item or zero quantity.");
                        return false;
                    }
                    
                    loNewEnt.setTransNox(fsTransNox);
                    loNewEnt.setEntryNox(lnCtr + 1);
                    loNewEnt.setDateModified(poGRider.getServerDate());

                    lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);

                    if (!lsSQL.equals("")){
                        if(poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0){
                            if(!poGRider.getErrMsg().isEmpty()){
                                setErrMsg(poGRider.getErrMsg());
                                return false;
                            }
                        } 
                    }
                }
            }
        } else{
            ArrayList<UnitInvTransferDetail> laSubUnit = loadTransactionDetail(poData.getTransNox());
            
            for (lnCtr = 0; lnCtr <= paDetail.size()-1; lnCtr++){
                loNewEnt = paDetail.get(lnCtr);
                
                if (!loNewEnt.getStockIDx().equals("")){
                    if (lnCtr <= laSubUnit.size()-1){
                        if (loNewEnt.getEntryNox() != lnCtr+1) loNewEnt.setEntryNox(lnCtr+1);
                        
                        lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, 
                                                (GEntity) laSubUnit.get(lnCtr), 
                                                "sStockIDx = " + SQLUtil.toSQL(loNewEnt.getValue(1)) +
                                                " AND nEntryNox = " + SQLUtil.toSQL(loNewEnt.getValue(2)));

                    } else{
                        loNewEnt.setStockIDx(fsTransNox);
                        loNewEnt.setEntryNox(lnCtr + 1);
                        loNewEnt.setDateModified(poGRider.getServerDate());
                        lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
                    }
                    
                    if (!lsSQL.equals("")){
                        if(poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0){
                            if(!poGRider.getErrMsg().isEmpty()){
                                setErrMsg(poGRider.getErrMsg());
                                return false;
                            }
                        } 
                    }
                } else{
                    for(int lnCtr2 = lnCtr; lnCtr2 <= laSubUnit.size()-1; lnCtr2++){
                        lsSQL = "DELETE FROM " + poDetail.getTable()+
                                " WHERE sStockIDx = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getStockIDx()) +
                                    " AND nEntryNox = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getEntryNox());

                        if (!lsSQL.equals("")){
                            if(poGRider.executeQuery(lsSQL, poDetail.getTable(), "", "") == 0){
                                if(!poGRider.getErrMsg().isEmpty()){
                                    setErrMsg(poGRider.getErrMsg());
                                    return false;
                                }
                            } 
                        }
                    }
                    break;
                }
            }
        }

        return true;
    }

    public boolean deleteTransaction(String string) {
        UnitInvTransferMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        String lsSQL = "DELETE FROM " + loObject.getTable() + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(string);
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        //delete detail rows
        lsSQL = "DELETE FROM " + poDetail.getTable() +
                " WHERE sTransNox = " + SQLUtil.toSQL(string);
        
        if (poGRider.executeQuery(lsSQL, poDetail.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        
        return lbResult;
    }

    public boolean closeTransaction(String string) {
        UnitInvTransferMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        //if it is already closed, just return true
        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CLOSED)) return true;
        
        if (!loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_OPEN)){
            setMessage("Unable to close closed/cancelled/posted/voided transaction.");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CLOSED) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("Unable to close transaction.");  
        } else lbResult = saveInvTrans();
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        
        
        return lbResult;
    }

    public boolean postTransaction(String string) {
        UnitInvTransferMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_VOID)){
            setMessage("Unable to close proccesed transaction.");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_POSTED) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("Tranasction was not posted.");  
        } else lbResult = postInvTrans();
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }

    public boolean voidTransaction(String string) {
        UnitInvTransferMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_VOID)){
            setMessage("Unable to close processed transaction.");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_VOID) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }

    public boolean cancelTransaction(String string) {
        UnitInvTransferMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_VOID)){
            setMessage("Unable to close processed transaction.");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else {
            if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CLOSED))
                lbResult = unsaveInvTrans();
        }
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }
    
    private void confirmSelectParent(int fnRow){
        ResultSet loRSParent;
        String [] laResult;
        
        loRSParent = poGRider.executeQuery(getSQ_Parent(paDetail.get(fnRow).getStockIDx()));
        if (MiscUtil.RecordCount(loRSParent) > 0){
            if (ShowMessageFX.YesNo("Item has no inventory but has parent unit.\n\n" + 
                                    "Do you want to use parent unit?",
                                    pxeModuleName, "Please confirm!!!")){

                String lsValue = showSelectParent(loRSParent,
                                                    (String) paDetailOthers.get(fnRow).getValue("sBarCodex"),
                                                    (String) paDetailOthers.get(fnRow).getValue("sDescript"),
                                                    (String) paDetailOthers.get(fnRow).getValue("sInvTypNm"),
                                                    (String) paDetailOthers.get(fnRow).getValue("sMeasurNm"));

                if (!lsValue.equals("")){
                    String [] lasValue = lsValue.split("»");

                    paDetailOthers.get(fnRow).setValue("sParentID", lasValue[0]);
                    paDetailOthers.get(fnRow).setValue("xParntQty", Integer.valueOf(paDetailOthers.get(fnRow).getValue("xParntQty").toString()) + 1);
                    paDetailOthers.get(fnRow).setValue("xQuantity", Integer.valueOf(paDetailOthers.get(fnRow).getValue("xQuantity").toString()) + Integer.parseInt(lasValue[1]));

                    paDetailOthers.get(fnRow).setValue("nQtyOnHnd", Integer.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()) + Integer.parseInt(lasValue[1]));
                    
                    if (paDetail.get(fnRow).getQuantity() == 0) setDetail(fnRow, "nQuantity", 1);
                }
            }
        }
    }
    
    private String showSelectParent(ResultSet foRS, 
                                     String fsBarCodex,
                                     String fsDescript,
                                     String fsInvTypNm,
                                     String fsMeasurNm){
        SubUnitController loSubUnit = new SubUnitController();
        loSubUnit.setParentUnits(foRS);

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("views/SubUnit.fxml"));
        fxmlLoader.setController(loSubUnit);
        
        try {

            loSubUnit.setBarCodex(fsBarCodex);
            loSubUnit.setDescript(fsDescript);
            loSubUnit.setBriefDsc(fsInvTypNm);
            loSubUnit.setInvTypNm(fsMeasurNm);

            Parent parent = fxmlLoader.load();

            Stage stage = new Stage();

            parent.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                }
            });
            parent.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    stage.setX(event.getScreenX() - xOffset);
                    stage.setY(event.getScreenY() - yOffset); 

                }
            });

            Scene scene = new Scene(parent);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setScene(scene);
            stage.showAndWait();
            
            if (!loSubUnit.isCancelled())
                return loSubUnit.getValue();

        } catch (IOException ex) {
            ShowMessageFX.Error(ex.getMessage(), pxeModuleName, "Please inform MIS department.");
            System.exit(1);
        }
        
        return "";
    }
    
    public boolean SearchDetail(int fnRow, int fnCol, String fsValue, boolean fbSearch, boolean fbByCode){
        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        
        JSONObject loJSON;
        ResultSet loRS;
        
        setErrMsg("");
        setMessage("");
        
        switch(fnCol){
            case 5:
                lsHeader = "Order No»Branch»Date»Inv. Type";
                lsColName = "sTransNox»sBranchNm»dTransact»sDescript";
                lsColCrit = "a.sTransNox»c.sBranchNm»a.dTransact»b.sDescript";
                lsSQL = getSQ_Requests();
                
                if (fbByCode){
                    if (paDetailOthers.get(fnRow).getValue("sOrderNox").equals(fsValue)) return true;
                    
                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox = " + SQLUtil.toSQL(fsValue));
                    
                    loRS = poGRider.executeQuery(lsSQL);
                    
                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                } else {
                    if (!fbSearch){
                        if (paDetailOthers.get(fnRow).getValue("sOrderNox").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            0);
                    } else
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            1);
                }
                
                if (loJSON != null){
                    setDetail(fnRow, fnCol, (String) loJSON.get("sTransNox"));
                    paDetailOthers.get(fnRow).setValue("sOrderNox", (String) loJSON.get("sTransNox"));
                    return true;
                } else{
                    setDetail(fnRow, fnCol, "");
                    paDetailOthers.get(fnRow).setValue("sOrderNox", "");
                    return false;
                }
            case 3:
                lsHeader = "Brand»Description»Unit»Model»Qty On Hnd»Inv. Type»Barcode»Stock ID";
                lsColName = "xBrandNme»sDescript»sMeasurNm»xModelNme»nQtyOnHnd»xInvTypNm»sBarCodex»sStockIDx";
                lsColCrit = "b.sDescript»a.sDescript»f.sMeasurNm»c.sDescript»e.nQtyOnHnd»d.sDescript»a.sBarCodex»a.sStockIDx";
                lsSQL = MiscUtil.addCondition(getSQ_Stocks(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
                
                if (fbByCode){
                    if (paDetailOthers.get(fnRow).getValue("sStockIDx").equals(fsValue)) return true;
                
                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
                    
                    loRS = poGRider.executeQuery(lsSQL);
                    
                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                }else {
                    if (!fbSearch){
                        if (paDetailOthers.get(fnRow).getValue("sBarCodex").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            6);
                    } else{
                        if (paDetailOthers.get(fnRow).getValue("sDescript").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            1);
                    }
                        
                }
                
                if (loJSON != null){
                    setDetail(fnRow, fnCol, (String) loJSON.get("sStockIDx"));
                    setDetail(fnRow, "nInvCostx", Double.valueOf((String) loJSON.get("nUnitPrce")));

                    paDetailOthers.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(fnRow).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
                    paDetailOthers.get(fnRow).setValue("sDescript", (String) loJSON.get("sDescript"));
                    paDetailOthers.get(fnRow).setValue("nQtyOnHnd", Integer.valueOf((String) loJSON.get("nQtyOnHnd")));
                    paDetailOthers.get(fnRow).setValue("nResvOrdr", Integer.valueOf((String) loJSON.get("nResvOrdr")));
                    paDetailOthers.get(fnRow).setValue("nBackOrdr", Integer.valueOf((String) loJSON.get("nBackOrdr")));
                    paDetailOthers.get(fnRow).setValue("nFloatQty", Integer.valueOf((String) loJSON.get("nFloatQty")));
                    paDetailOthers.get(fnRow).setValue("nLedgerNo", Integer.valueOf((String) loJSON.get("nLedgerNo")));
                    paDetailOthers.get(fnRow).setValue("sInvTypNm", (String) loJSON.get("sInvTypNm"));
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));

                    //if (Integer.valueOf((String) loJSON.get("nQtyOnHnd")) > 0) 
                    //    setDetail(fnRow, "nQuantity", 1);
                    //else confirmSelectParent(fnRow);

                    return true;
                } else{
                    setDetail(fnRow, fnCol, "");
                    setDetail(fnRow, "nInvCostx", 0.00);
                    setDetail(fnRow, "nQuantity", 0);
                    
                    paDetailOthers.get(fnRow).setValue("sStockIDx", "");
                    paDetailOthers.get(fnRow).setValue("sBarCodex", "");
                    paDetailOthers.get(fnRow).setValue("sDescript", "");
                    paDetailOthers.get(fnRow).setValue("sStockIDx", "");
                    paDetailOthers.get(fnRow).setValue("sParentID", "");
                    paDetailOthers.get(fnRow).setValue("nQtyOnHnd", 0);
                    paDetailOthers.get(fnRow).setValue("nResvOrdr", 0);
                    paDetailOthers.get(fnRow).setValue("nBackOrdr", 0);
                    paDetailOthers.get(fnRow).setValue("nFloatQty", 0);
                    paDetailOthers.get(fnRow).setValue("nLedgerNo", 0);
                    paDetailOthers.get(fnRow).setValue("xQuantity", 0);
                    return false;
                }
            case 4:
                lsHeader = "Barcode»Description»Inv. Type»Brand»Model»Stock ID";
                lsColName = "sBarCodex»sDescript»xInvTypNm»xBrandNme»xModelNme»sStockIDx";
                lsColCrit = "a.sBarCodex»a.sDescript»d.sDescript»b.sDescript»c.sDescript»a.sStockIDx";
                lsSQL = MiscUtil.addCondition(getSQ_Stocks(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
                
                if (fbByCode){
                    if (paDetail.get(fnRow).getOrigIDxx().equals(fsValue)) return true;
                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
                    
                    loRS = poGRider.executeQuery(lsSQL);
                    
                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                } else {
                    loJSON = showFXDialog.jsonSearch(poGRider, 
                                                        lsSQL, 
                                                        fsValue, 
                                                        lsHeader, 
                                                        lsColName, 
                                                        lsColCrit, 
                                                        fbSearch ? 2 : 1);
                }
                
                if (loJSON != null){
                    setDetail(fnRow, fnCol, (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(fnRow).setValue("sOrigCode", (String) loJSON.get("sBarCodex"));
                    paDetailOthers.get(fnRow).setValue("sOrigDesc", (String) loJSON.get("sDescript"));
                    
                    return true;
                } else{
                    setDetail(fnRow, fnCol, "");
                    paDetailOthers.get(fnRow).setValue("sOrigCode", "");
                    paDetailOthers.get(fnRow).setValue("sOrigDesc", "");
                    
                    return false;
                }
            default:
                return false;
        }
    }
    
    public boolean SearchDetail(int fnRow, String fsCol, String fsValue, boolean fbSearch, boolean fbByCode){
        return SearchDetail(fnRow, poDetail.getColumn(fsCol), fsValue, fbSearch, fbByCode);
    }
    
    public boolean SearchMaster(int fnCol, String fsValue, boolean fbByCode){       
        switch(fnCol){
            case 4: //sDestinat
                XMBranch loBranch = new XMBranch(poGRider, psBranchCd, true);
                if (loBranch.browseRecord(fsValue, fbByCode)){
                    setMaster(fnCol, (String) loBranch.getMaster("sBranchCd"));
                    MasterRetreived(fnCol);
                    return true;
                }

        }
        return false;
    }
    
    public boolean SearchMaster(String fsCol, String fsValue, boolean fbByCode){
        return SearchMaster(poData.getColumn(fsCol), fsValue, fbByCode);
    }
    
    public void setMaster(int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poData.getColumn("sTransNox") ||
                fnCol == poData.getColumn("nEntryNox") ||
                fnCol == poData.getColumn("cTranStat") ||
                fnCol == poData.getColumn("sModified") ||
                fnCol == poData.getColumn("dModified"))){
                
                if (fnCol == poData.getColumn("nFreightx") ||
                    fnCol == poData.getColumn("nTranTotl") ||
                    fnCol == poData.getColumn("nDiscount")){
                    if (foData instanceof Number){
                        poData.setValue(fnCol, foData);
                    }else poData.setValue(fnCol, 0.00);
                } else poData.setValue(fnCol, foData);
                
                MasterRetreived(fnCol);
            }
        }
    }

    public void setMaster(String fsCol, Object foData) {
        setMaster(poData.getColumn(fsCol), foData);
    }

    public Object getMaster(int fnCol) {
        if(pnEditMode == EditMode.UNKNOWN)
            return null;
        else{
            return poData.getValue(fnCol);
      }
    }

    public Object getMaster(String fsCol) {
        return getMaster(poData.getColumn(fsCol));
    }

    public String getMessage() {
        return psWarnMsg;
    }

    public void setMessage(String string) {
        psWarnMsg = string;
    }

    public String getErrMsg() {
        return psErrMsgx;
    }

    public void setErrMsg(String string) {
        psErrMsgx = string;
    }

    public void setBranch(String string) {
        psBranchCd = string;
    }

    public void setWithParent(boolean bln) {
        pbWithParent = bln;
    }

    public String getSQ_Master() {
        return MiscUtil.makeSelect(new UnitInvTransferMaster());
    }
    
    private String getSQ_Detail(){        
        return "SELECT" + 
                    "  a.sTransNox" +
                    ", a.nEntryNox" +
                    ", a.sStockIDx" +
                    ", a.sOrigIDxx" +
                    ", a.sOrderNox" +
                    ", a.nQuantity" +
                    ", a.nInvCostx" +
                    ", a.sRecvIDxx" +
                    ", a.nReceived" +
                    ", a.sNotesxxx" +
                    ", a.dModified" +
                    ", b.nQtyOnHnd" + 
                    ", b.nQtyOnHnd + a.nQuantity xQtyOnHnd" + 
                    ", b.nResvOrdr" +
                    ", b.nBackOrdr" +
                    ", b.nFloatQty" +
                    ", b.nLedgerNo" +
                    ", c.sBarCodex" +
                    ", c.sDescript" +
                    ", IFNULL(d.sBarCodex, '') xBarCodex" + 
                " FROM Inv_Transfer_Detail a" + 
                        " LEFT JOIN Inventory d" + 
                            " ON a.sOrigIDxx = d.sStockIDx" + 
                    ", Inv_Master b" +
                        " LEFT JOIN Inventory c" + 
                            " ON b.sStockIDx = c.sStockIDx" + 
                " WHERE a.sStockIDx = b.sStockIDx" + 
                    " AND b.sBranchCD = " + SQLUtil.toSQL(psBranchCd) + 
                " ORDER BY a.nEntryNox";
    }
    
    public int ItemCount(){
        return paDetail.size();
    }
    
     public Inventory GetInventory(String fsValue, boolean fbByCode, boolean fbSearch){        
        Inventory instance = new Inventory(poGRider, psBranchCd, fbSearch);
        instance.BrowseRecord(fsValue, fbByCode, false);
        return instance;
    }
     
    public XMBranch GetBranch(String fsValue, boolean fbByCode){
       if (fbByCode && fsValue.equals("")) return null;

       XMBranch instance  = new XMBranch(poGRider, psBranchCd, true);
       if (instance.browseRecord(fsValue, fbByCode))
           return instance;
       else
           return null;
    }
    
    private Connection setConnection(){
        Connection foConn;
        
        if (pbWithParent){
            foConn = (Connection) poGRider.getConnection();
            if (foConn == null) foConn = (Connection) poGRider.doConnect();
        }else foConn = (Connection) poGRider.doConnect();
        
        return foConn;
    }
    
    public int getEditMode(){return pnEditMode;}
    
    private String getSQ_InvTransfer(){        
        String lsTranStat = String.valueOf(pnTranStat);
        String lsCondition = "";
        String lsSQL = "SELECT " +
                            "  a.sTransNox" +
                            ", b.sBranchNm" +
                            ", a.dTransact" +
                            ", c.sBranchNm" + 
                        " FROM Inv_Transfer_Master a" +
                            " LEFT JOIN Branch b" +
                                " ON a.sDestinat = b.sBranchCd" +
                            " LEFT JOIN Branch c" +
                                " ON LEFT(a.sTransNox, 4) = c.sBranchCd";
                        //" WHERE a.sTransNox LIKE " + SQLUtil.toSQL(psBranchCd + "%");
        
        if (lsTranStat.length() == 1) {
            lsCondition = "a.cTranStat = " + SQLUtil.toSQL(lsTranStat);
        } else {
            for (int lnCtr = 0; lnCtr <= lsTranStat.length() -1; lnCtr++){
                lsCondition = lsCondition + SQLUtil.toSQL(String.valueOf(lsTranStat.charAt(lnCtr))) + ",";
            }
            lsCondition = "(" + lsCondition.substring(0, lsCondition.length()-1) + ")";
            lsCondition = "a.cTranStat IN " + lsCondition;
        }
        
        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        return lsSQL;
    }
    
    private String getSQ_Requests(){
        return "SELECT " +
                    "  a.sTransNox" +
                    ", c.sBranchNm" + 
                    ", b.sDescript" + 
                    ", a.dTransact" + 
                " FROM Inv_Transfer_Master a" + 
                    " LEFT JOIN Inv_Type b" + 
                        " ON a.sInvTypCd = b.sInvTypCd" + 
                    " LEFT JOIN Branch c" + 
                        " ON a.sBranchCd = c.sBranchNm";
    }
    
    private String getSQ_Parent(String fsStockIDx){
        return "SELECT" +
                    "  a.sStockIDx" + 
                    ", a.sItmSubID" + 
                    ", a.nQuantity" + 
                    ", c.sBarCodex" + 
                    ", c.sDescript" + 
                    ", b.nQtyOnHnd" + 
                    ", d.sMeasurNm" +
                " FROM Inventory_Sub_Unit a" + 
                    ", Inv_Master b" + 
                        " LEFT JOIN Inventory c" + 
                            " ON b.sStockIDx = c.sStockIDx" + 
                        " LEFT JOIN Measure d" +
                            " ON c.sMeasurID = d.sMeasurID" +
                " WHERE a.sStockIDx = b.sStockIDx" + 
                    " AND b.sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                    " AND a.sItmSubID = " + SQLUtil.toSQL(fsStockIDx) + 
                    " AND b.nQtyOnHnd > 0";
    }
    
    private String getSQ_StocksByRequest(){
        return "SELECT " +
                    "  a.sStockIDx" +
                    ", a.sBarCodex" + 
                    ", a.sDescript" + 
                    ", a.sBriefDsc" + 
                    ", a.sAltBarCd" + 
                    ", a.sCategCd1" + 
                    ", a.sCategCd2" + 
                    ", a.sCategCd3" + 
                    ", a.sCategCd4" + 
                    ", a.sBrandCde" + 
                    ", a.sModelCde" + 
                    ", a.sColorCde" + 
                    ", a.sInvTypCd" + 
                    ", a.nUnitPrce" + 
                    ", a.nSelPrice" + 
                    ", a.nDiscLev1" + 
                    ", a.nDiscLev2" + 
                    ", a.nDiscLev3" + 
                    ", a.nDealrDsc" + 
                    ", a.cComboInv" + 
                    ", a.cWthPromo" + 
                    ", a.cSerialze" + 
                    ", a.cUnitType" + 
                    ", a.cInvStatx" + 
                    ", a.sSupersed" + 
                    ", a.cRecdStat" + 
                    ", b.sDescript" + 
                    ", c.sDescript" + 
                    ", d.sDescript" + 
                " FROM Inventory a" + 
                        " LEFT JOIN Brand b" + 
                            " ON a.sBrandCde = b.sBrandCde" + 
                        " LEFT JOIN Model c" + 
                            " ON a.sModelCde = c.sModelCde" + 
                        " LEFT JOIN Inv_Type d" + 
                            " ON a.sInvTypCd = d.sInvTypCd" + 
                    ", Inv_Stock_Request_Detail e" + 
                " WHERE a.sStockIDx = e.sStockIDx" + 
                    " AND a.sStockIDx = e.sStockIDx";
    }
    
    
    private String getSQ_Stocks(){
        return "SELECT " +
                    "  a.sStockIDx" +
                    ", a.sBarCodex" + 
                    ", a.sDescript" + 
                    ", a.sBriefDsc" + 
                    ", a.sAltBarCd" + 
                    ", a.sCategCd1" + 
                    ", a.sCategCd2" + 
                    ", a.sCategCd3" + 
                    ", a.sCategCd4" + 
                    ", a.sBrandCde" + 
                    ", a.sModelCde" + 
                    ", a.sColorCde" + 
                    ", a.sInvTypCd" + 
                    ", a.nUnitPrce" + 
                    ", a.nSelPrice" + 
                    ", a.nDiscLev1" + 
                    ", a.nDiscLev2" + 
                    ", a.nDiscLev3" + 
                    ", a.nDealrDsc" + 
                    ", a.cComboInv" + 
                    ", a.cWthPromo" + 
                    ", a.cSerialze" + 
                    ", a.cUnitType" + 
                    ", a.cInvStatx" + 
                    ", a.sSupersed" + 
                    ", a.cRecdStat" + 
                    ", b.sDescript xBrandNme" + 
                    ", c.sDescript xModelNme" + 
                    ", d.sDescript xInvTypNm" + 
                    ", e.nQtyOnHnd" +
                    ", e.nResvOrdr" +
                    ", e.nBackOrdr" + 
                    ", e.nFloatQty" + 
                    ", IFNULL(e.nLedgerNo, 0) nLedgerNo" + 
                    ", f.sMeasurNm" +
                " FROM Inventory a" + 
                        " LEFT JOIN Brand b" + 
                            " ON a.sBrandCde = b.sBrandCde" + 
                        " LEFT JOIN Model c" + 
                            " ON a.sModelCde = c.sModelCde" + 
                        " LEFT JOIN Inv_Type d" + 
                            " ON a.sInvTypCd = d.sInvTypCd" + 
                        " LEFT JOIN Measure f" +
                            " ON a.sMeasurID = f.sMeasurID" +
                    ", Inv_Master e" + 
                " WHERE a.sStockIDx = e.sStockIDx" + 
                    " AND e.sBranchCd = " + SQLUtil.toSQL(psBranchCd);
    }
    
    public void printColumnsMaster(){poData.list();}
    public void printColumnsDetail(){poDetail.list();}
    public void setTranStat(int fnValue){this.pnTranStat = fnValue;}
    
    //callback methods
    public void setCallBack(IMasterDetail foCallBack){
        poCallBack = foCallBack;
    }
    
    private void MasterRetreived(int fnRow){
        if (poCallBack == null) return;
        
        poCallBack.MasterRetreive(fnRow);
    }
    
    private void DetailRetreived(int fnRow){
        if (poCallBack == null) return;
        
        poCallBack.DetailRetreive(fnRow);
    }
    
    //Member Variables
    private GRider poGRider = null;
    private String psUserIDxx = "";
    private String psBranchCd = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private boolean pbWithParent = false;
    private int pnEditMode;
    private int pnTranStat = 0;
    private IMasterDetail poCallBack;
    
    private UnitInvTransferMaster poData = new UnitInvTransferMaster();
    private UnitInvTransferDetail poDetail = new UnitInvTransferDetail();
    private ArrayList<UnitInvTransferDetail> paDetail;
    private ArrayList<UnitInvTransferDetailOthers> paDetailOthers;
    
    private final String pxeModuleName = "InvTransfer";
    private double xOffset = 0; 
    private double yOffset = 0;
}
