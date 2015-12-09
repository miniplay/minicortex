package com.miniplay.custom.observers;

import com.miniplay.common.GlobalFunctions;

/**
 *
 * Created by ret on 7/12/15.
 */
public class ContainerObserver {

    public ContainerObserver() {

    }

    public void run() {
        System.out.println(GlobalFunctions.PREPEND_OUTPUT_OBSERVERS + "Contanier observer running...");
    }
}
