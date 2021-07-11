package com.ranranx.aolie.query.modal.querycol;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/27 0027 15:09
 **/
public class SerialProvider {
    /**
     * 生成节点信息
     */
    private long idSerial = 1L;
    /**
     * 用于生成上级节点的ID
     */
    private long virtualId = 20000L;

    public long getNextSerial() {
        return idSerial++;
    }

    public long getNextVirtualSerial() {
        return virtualId++;
    }


}
