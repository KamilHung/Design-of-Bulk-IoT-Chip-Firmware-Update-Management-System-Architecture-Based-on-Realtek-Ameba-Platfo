#include "FreeRTOS.h"
#include "task.h"
#include "diag.h"
#include "main.h"
#include <example_entry.h>

extern void console_init(void);

#define PRODUCT                 "H1"
#define FW_SET_VERSION          "10101"


/**
  * @brief  Main program.
  * @param  None
  * @retval None
  */
void main(void)
{
	/* Initialize log uart and at command service */
	console_init();	

	/* pre-processor of application example */
	//pre_example_entry();
	/* wlan intialization */
#if defined(CONFIG_WIFI_NORMAL) && defined(CONFIG_NETWORK)
	//wlan_network();        // Source flow
#endif
        printf("Now Product %s\n", PRODUCT);
        printf("Now FW version %s\n", FW_SET_VERSION);
        
        wlan_networkpaper(); // paper flow
	/* Execute application example */
	example_entry();

    	/*Enable Schedule, Start Kernel*/
#if defined(CONFIG_KERNEL) && !TASK_SCHEDULER_DISABLED
	#ifdef PLATFORM_FREERTOS
	vTaskStartScheduler();
	#endif
#else
	RtlConsolTaskRom(NULL);
#endif
}
