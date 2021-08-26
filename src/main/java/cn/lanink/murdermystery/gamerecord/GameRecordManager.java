package cn.lanink.murdermystery.gamerecord;

import cn.lanink.murdermystery.MurderMystery;
import cn.lanink.murdermystery.gamerecord.roundrecord.RoundRecord;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author lt_name
 */
public abstract class GameRecordManager {

    protected final MurderMystery murderMystery = MurderMystery.getInstance();
    public static int roundRecordCount = 0;

    public abstract void addRoundRecord(RoundRecord roundRecord);

    public abstract RoundRecord getRoundRecord(int id);

    public abstract List<RoundRecord> getRoundRecordList();

    public abstract List<RoundRecord> getRoundRecordListByPlayer(@NotNull String name);

}
