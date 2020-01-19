#include "FreeRTOS.h"
#include "task.h"
#include "semphr.h" 

#include "main.h"
#include "main_test.h"
#include "wifi_conf.h"
#include "wlan_intf.h"
#include "wifi_constants.h"
#include <platform/platform_stdlib.h>
#include "lwip_netconf.h"
#include <lwip/sockets.h>

#include "flash_api.h"
#include <sys.h>
#include <device_lock.h>
#include "ota_8195a.h"
#include "lwip/netdb.h"

#define STACKSIZE               (512)
#define PRODUCT                 "H1"
#define FW_SET_VERSION          "10101"
#define KAMIL_DATA              0x9100  //flash save data address
#define KAMIL_DEBUG             0

typedef struct {
        unsigned char setup_code[9];
        unsigned char serial_number[20];
} init_data_t;

extern struct netif xnetif[NET_IF_NUM];
static xSemaphoreHandle uart_rx_interrupt_semaxlink = NULL;
int server_fd = -1;
static flash_t flash_ota;
static init_data_t init_data;
unsigned char *array1;

unsigned char array2[] = {
        0xAA, //Header
        0x02, //length
        0x00, //start or result
        0x00, //checksum
        0x55  //End
};

/*****************************
* function define
*****************************/
void kamil_ota_platform_reset(void);
int kamil_write_ota_addr_to_system_data(flash_t *flash, uint32_t ota_addr);
int kamil_update_ota_local();
int kamil_update_ota_local_task(void);
char checksum(int total);
int combinationArray(unsigned char *mac, char *sc, char *sn);
int protocolAnalysis(char* buf);

//---------------------------------------------------------------------
static void* update_malloc(unsigned int size)
{
        return pvPortMalloc(size);
}

static void update_free(void *buf)
{
        vPortFree(buf);
}

void printArray(const unsigned char *ptr, size_t length)         
{         
        //for statement to print values using array
        printf("printArray: ");
        size_t i = 0;
        for(; i < length; ++i){
                printf("0x%2x ", ptr[i]);
        }
        printf("\n");
}   

void printString(char note[], const unsigned char *ptr)
{         
        //for statement to print values using array
        printf("%s, printString: \n", note);
        for(; *ptr != '\0'; ++ptr){
                printf("%c ", *ptr);
        }
        printf("\n");
}

void printHex(const unsigned char *ptr)
{         
        //for statement to print values using array
        printf("printHex: ");
        for(; *ptr != '\0'; ++ptr){
                printf("0x%2x, ", *ptr);
        }
        printf("\n");
}

int arrayCompare(const unsigned char array1[], const unsigned char array2[]){
#if KAMIL_DEBUG 
        printf("arrayCompare:\n");
#endif
        int i = 0;
        for(i = 0; array1[i] != '\0' || array2[i] != '\0'; i++){
                if(array1[i] == '\0' || array2[i] == '\0'){
                        return 0;
                }
                if(array1[i] != array2[i]){
                        return 0;
                }
        }
        return 1;
}

int pointCount(const unsigned char *ptr){
        printf("pointCount\n");
        int count = 0;
        for(; *ptr != '\0'; ++ptr){
                count++;
        }
        return count;
}

//---------------------------------------------------------------------
/* Ameba send Server
        0xAA    //Header
        Length  //1byte
        MAC     //6byte
        Product //1byte
        Ver     //3byte
        SC      //1+8byte
        SN      //1+?byte
        Checksum//1byte
        0x55    //End
*/

static char checksum(int total){
        int i = 0;
        char sum = 0x00;
        for (i = 1; i <= total - 3; i++){
                sum += array1[i] & 0xFF;
        }
        return sum;
}

static int combinationArray(unsigned char *mac, char *sc, char *sn){ //flash sc and sn
        int length = 1;
        int mac_length = 6;
        int product_length = 1;
        int ver_length = 3;
        int sc_length = 1 + 8;
        int flash_sn_length = (sn == NULL)? 0:strlen(sn);
        int sn_length = 1 + flash_sn_length;
        int protocol_length = length + mac_length + product_length + ver_length + sc_length + sn_length;
        int total = protocol_length + 3; //Header + End + checksum

        array1 = (unsigned char*)malloc(sizeof(char) * total); //malloc array

        array1[0] = 0xAA;
        array1[1] = (char)protocol_length;

        //array1[2] ~ array1[7] = mac
        int i = 0;
        for (i = 0; i < 6; i++){
                array1[2 + i] = *(mac + i);
        }
        printf("\n");

        if (PRODUCT == "H1"){ //Product
                array1[8] = 0x00;
        }
        else if (PRODUCT == "M1"){
                array1[8] = 0x01;
        }
        else if (PRODUCT == "C1"){
                array1[8] = 0x02;
        }
        else{
                array1[8] = 0x00;
        }

        //ver array1[9]~array1[11]
        //10101 = 0x31 0x31 0x31
        /*char *p = FW_SET_VERSION;
        for (i = 0; i < strlen(FW_SET_VERSION); i += 2){
                array1[9 + i] = *(p + i); //ver
        }*/

        if (FW_SET_VERSION == "10101"){
                array1[9]  = 0x01;
                array1[10] = 0x01;
                array1[11] = 0x01;
        }
        else if (FW_SET_VERSION == "10102"){
                array1[9]  = 0x01;
                array1[10] = 0x01;
                array1[11] = 0x02;
        }

        //SC array1[12]~array1[20]
        if (sc != NULL){
                array1[12] = 0x01;
                for (i = 13; i < 21; i++){
                        array1[i] = *(sc + i - 13);
                }
        }
        else{
                for (i = 12; i < 21; i++){
                        array1[i] = 0x00;
                }
        }

        //SN array1[21~21+flash_sn_length]
        if (sn != NULL){
                array1[21] = (char)flash_sn_length;
                for (i = 22; i < 22 + flash_sn_length; i++){
                        array1[i] = *(sn + i - 22);
                }
        }
        else{
                for (i = 21; i < 22 + flash_sn_length; i++){
                        array1[i] = 0x00;
                }
        }

        //Checksum
        array1[total - 2] = checksum(total);
        //End
        array1[total - 1] = 0x55;

        printArray(array1, total); //print Array

        return total;
}

/* Server send Ameba
        0xAA    //Header
        Length  //1byte
        SC      //1+8byte
        SN      //1+?byte
        Update  //1byte
        Checksum//1byte
        0x55    //End
*/
static int protocolAnalysis(char* buf){
        int total = buf[1] + 3; //Header + End + checksum
        unsigned char *sc = NULL, *sn = NULL;
        sc = (unsigned char*)malloc(sizeof(unsigned char) * 8);
        sn = (unsigned char*)malloc(sizeof(unsigned char) * buf[11]);
        int i;
        if (buf[0] == (char)0xAA){
                if (buf[2] == 0x01){ //SC
                        for (i = 3; i < 11; i++){
                                *(sc + (i - 3)) = buf[i];
                        }
#if KAMIL_DEBUG
                        printString("PA SC", sc);
#endif
                }
                else{
                        sc = init_data.setup_code;
                }

                if (buf[11] > 0x00){ //SN
                        for (i = 12; i < 12 + buf[11]; i++){
                                *(sn + (i - 12)) = buf[i];
                        }
#if KAMIL_DEBUG
                        printString("PA SN", sn);
#endif
                }
                else{
                        sn = init_data.serial_number;
                }
                
                if (!arrayCompare(init_data.setup_code, sc) || !arrayCompare(init_data.serial_number, sn)){
                        if(*sc != NULL){
                                for(i = 0; i < 8; i++){
                                        init_data.setup_code[i] = *(sc + i);
#if KAMIL_DEBUG
                                        printf("%d, %2x, %2x\n", i, init_data.setup_code[i], *(sc + i));
#endif
                                }
                        }
                        
                        if(*sn != NULL){
                                for(i = 0; i < buf[11]; i++){
                                        init_data.serial_number[i] = *(sn + i);
#if KAMIL_DEBUG
                                        printf("%d, %2x, %2x\n", i, init_data.serial_number[i], *(sn + i));
#endif
                                }
                        }
#if KAMIL_DEBUG
                        printString("init_data.setup_code", init_data.setup_code);
                        printString("init_data.serial_number", init_data.serial_number);
                        printf("\n");
#endif
                        return 1; //update flash
                }
                else{
                        return 0;
                }
        }else{
                printf("buf[0] != 0xAA\n");
        }
        return 0;
}

void xinit_thread(void *param)
{
        printf("fn %s  line %d  start air kiss\r\n", __FUNCTION__,__LINE__);

#if CONFIG_INIT_NET
#if CONFIG_LWIP_LAYER
        /* Initilaize the LwIP stack */
        LwIP_Init();
#endif
#endif
        printf("fn %s  line %d  start air kiss  1\r\n", __FUNCTION__,__LINE__);
 
#if CONFIG_WLAN
        wifi_on(RTW_MODE_STA);
        printf("\n\r%s(%d), Available heap 0x%x", __FUNCTION__, __LINE__, xPortGetFreeHeapSize());	
#endif

#if CONFIG_INTERACTIVE_MODE
        /* Initial uart rx swmaphore */
        vSemaphoreCreateBinary(uart_rx_interrupt_sema);
        xSemaphoreTake(uart_rx_interrupt_semaxlink, 1/portTICK_RATE_MS);
        start_interactive_mode();
#endif	
 
#if 1   //寫死一個默認的ssid 和 密碼
        int ret, count = 0;
        do{
                ret = wifi_connect("Kamil-NB", RTW_SECURITY_WPA_AES_PSK, 
                                    "a1234567", strlen("Kamil-NB"), strlen("a1234567"), 0, NULL);
                count++;
                vTaskDelay(1000);
        }while((ret!= RTW_SUCCESS) && (count < 5));

        if(count >= 5){
                printf("\n\rERROR: Operation failed!");
        }else{
                printf("\r\nWiFi Connected ok!\n");
        }
        LwIP_DHCP(0, 0);
#endif
        /////////////////////////////////////
        /* Kill init thread after all init tasks done */
        vTaskDelete(NULL); 
}

#define SERVER_HOST     "192.168.137.1" //PC wifi Direct Virtual Adapter IP
#define SERVER_PORT     1234
#define RESOURCE        ""
#define BUFFER_SIZE     512
#define RECV_TO         5000    // ms

static void socket_select_thread(void *param)
{
        int ret;
        struct sockaddr_in server_addr;
        struct hostent *server_host;
        flash_t flash; // flash read and write
        uint8_t *mac = LwIP_GetMAC(&xnetif[0]); // Get MAC address
        printf("\n=====Read MAC Address => %02x:%02x:%02x:%02x:%02x:%02x======\n",
                mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]); // Read Mac Address

        // Delay to wait for IP by DHCP
        vTaskDelay(10000);
#if 1        
        memset(&init_data, 0x00, sizeof(init_data));
        printf("\nRead flash\n");
        flash_stream_read(&flash, KAMIL_DATA, sizeof(init_data), &init_data); //read flash
        printString("read flash sc", init_data.setup_code);
        printString("read flash sn", init_data.serial_number);
#endif   
        int total = combinationArray((unsigned char *)mac, init_data.setup_code, init_data.serial_number);

        if((server_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0) { //socket init
                printf("ERROR: socket\n");
                goto exit;
        }
        else {
                int recv_timeout_ms = RECV_TO;
#if defined(LWIP_SO_SNDRCVTIMEO_NONSTANDARD) && (LWIP_SO_SNDRCVTIMEO_NONSTANDARD == 0)	// lwip 1.5.0
                struct timeval recv_timeout;
                recv_timeout.tv_sec = recv_timeout_ms / 1000;
                recv_timeout.tv_usec = recv_timeout_ms % 1000 * 1000;
                setsockopt(server_fd, SOL_SOCKET, SO_RCVTIMEO, &recv_timeout, sizeof(recv_timeout));
#else	// lwip 1.4.1
                setsockopt(server_fd, SOL_SOCKET, SO_RCVTIMEO, &recv_timeout_ms, sizeof(recv_timeout_ms));
#endif
		}

		server_addr.sin_family = AF_INET;
		server_addr.sin_port = htons(SERVER_PORT);

		server_host = gethostbyname(SERVER_HOST);
		memcpy((void *) &server_addr.sin_addr, (void *) server_host->h_addr, server_host->h_length);

		if(connect(server_fd, (struct sockaddr *) &server_addr, sizeof(server_addr)) == 0) {
                        printf("\n\nSocket Connect ok\n");
                        unsigned char buf[50];
                        unsigned char fw_buf[BUFFER_SIZE];
                        int read_size = 0, resource_size = 0, content_len = 0, header_removed = 0;

                        printf("Send Protocol\n");
                        if(read_size = write(server_fd, array1, total) == -1){//Send Protocol
                                perror("write");
                                exit(1);
                        }
                        
                        vTaskDelay(500); //delay 500ms
                        memset(buf, 0x00, sizeof(buf));

                        int read_count = 0;
                        while(read_size == 0){
                                read_size = read(server_fd, buf, sizeof(buf));
                                if(read_size == -1){
                                        perror("read");
                                        exit(1);
                                }
                                read_count++;
                        }
#if 1
                        printf("read_size = %d, read_count = %d\n", read_size, read_count);
#endif
                        printArray(buf, read_size); //print Array
                        
                        int i;
                        int refresh = protocolAnalysis(buf);
                        printf("refresh = %d\n", refresh);
                        if(refresh){
#if 1
                                flash_erase_sector(&flash, KAMIL_DATA);//erase flash 0x9100 data
                                flash_stream_write(&flash, KAMIL_DATA, sizeof(init_data), &init_data); //write flash
                                printf("flash write OK!\n");
                                printf("\nRead flash\n");
                                flash_stream_read(&flash, KAMIL_DATA, sizeof(init_data), &init_data); //read flash                             
                                printString("write flash sc", init_data.setup_code);
                                printString("write flash sn", init_data.serial_number);
#endif
                        }

                        int start = 0;
                        if (buf[read_size - 3] == 0x01){
                                printf("Start upgrade\n");
                                array2[2] = 0x01;
                                array2[3] = (array2[1] + array2[2]) & 0xFF;
                                write(server_fd, array2, sizeof(array2)); //Send Protocol => Start
                                kamil_update_ota_local_task(); //Upgrade FW
                        }
                        else{
                                printf("non-upgrade\n");
                                array2[2] = 0x00;
                                array2[3] = (array2[1] + array2[2]) & 0xFF;
                                write(server_fd, array2, sizeof(array2)); //Send Protocol => no upgrade
                        }
		}
		else {
                        printf("Socket ERROR: connect fail\n");
		}
		printf("CloseConnect\n");
		if (server_fd >= 0){
                        close(server_fd);
		}
		vTaskDelete(NULL);
        
exit:
		printf("Error exit\n");
		if (server_fd >= 0){
                        close(server_fd);
		}
		vTaskDelete(NULL);
}

static int kamil_update_ota_local_task(void)
{
        unsigned char *buf, *alloc;
        _file_checksum *file_checksum;
        int read_bytes = 0, size = 0, i = 0;
        uint32_t address, flash_checksum=0;
        uint32_t NewImg2Len = 0, NewImg2Addr = 0, file_info[3];
        int ret = -1 ;

        printf("\n\r[%s] Update task start", __FUNCTION__);
        alloc = update_malloc(BUF_SIZE+4);
        if(!alloc){
                printf("\n\r[%s] Alloc buffer failed", __FUNCTION__);
                goto update_ota_exit;
        }
        buf = &alloc[4];
        file_checksum = (void*)alloc;

        if(server_fd == -1){
                goto update_ota_exit;
        }

        NewImg2Addr = update_ota_prepare_addr();
        if(NewImg2Addr == -1){
                goto update_ota_exit;
        }

        //Clear file_info
        memset(file_info, 0, sizeof(file_info));

        if(file_info[0] == 0){
                read_bytes = read(server_fd, file_info, sizeof(file_info));
                // !X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X
                // !W checksum !W padding 0 !W file size !W
                // !X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X!X
                printf("\n\r[%s] info %d bytes", __FUNCTION__, read_bytes);
                printf("\n\r[%s] tx chechsum 0x%x, file_info[1] 0x%x, file size 0x%x",
                       __FUNCTION__, file_info[0], file_info[1], file_info[2]);
                         
                if(file_info[2] == 0){
                        printf("\n\r[%s] No checksum and file size", __FUNCTION__);
                        goto update_ota_exit;
                }
        }

#if SWAP_UPDATE
        NewImg2Addr = update_ota_swap_addr(file_info[2], NewImg2Addr);
        if(NewImg2Addr == -1){
                goto update_ota_exit;
        }	
#endif
        NewImg2Len = update_ota_erase_upg_region(file_info[2], NewImg2Len, NewImg2Addr);
        if(NewImg2Len == -1){
                goto update_ota_exit;
        }

        // reset
        file_checksum->u = 0;
        // Write New Image 2 sector
        if(NewImg2Addr != ~0x0){
                address = NewImg2Addr;
                printf("\n\rStart to read data %d bytes\r\n", NewImg2Len);
                while(1){
                        memset(buf, 0, BUF_SIZE);
                        read_bytes = read(server_fd, buf, BUF_SIZE);
                        if(read_bytes == 0) 
                                break; // Read end
                        if(read_bytes < 0){
                                printf("\n\r[%s] Read socket failed", __FUNCTION__);
                                goto update_ota_exit;
                        }

                        if(read_bytes<4)
                                printf("\n\r[%s] Recv small packet", __FUNCTION__);
                        printf(".");

                        if((size+read_bytes)>NewImg2Len){
                                printf("\n\r[%s] Redundant bytes received", __FUNCTION__);
                                read_bytes = NewImg2Len-size;	
                        }

                        device_mutex_lock(RT_DEV_LOCK_FLASH);
                        if(flash_stream_write(&flash_ota, address + size, read_bytes, buf) < 0){
                                printf("\n\r[%s] Write stream failed", __FUNCTION__);
                                device_mutex_unlock(RT_DEV_LOCK_FLASH);
                                goto update_ota_exit;
                        }
                        device_mutex_unlock(RT_DEV_LOCK_FLASH);
                        size += read_bytes;

                        file_checksum->c[0] = alloc[4+read_bytes-4];      // checksum attached at file end
                        file_checksum->c[1] = alloc[4+read_bytes-3];
                        file_checksum->c[2] = alloc[4+read_bytes-2];
                        file_checksum->c[3] = alloc[4+read_bytes-1];

                        if(size == NewImg2Len)
                                break;
                }
                printf("\n\rRead data finished\r\n");

                // read flash data back and calculate checksum
                for(i = 0; i < size-4; i += BUF_SIZE){
                        int k;
                        int rlen = (size-4-i) > BUF_SIZE ? BUF_SIZE : (size-4-i);
                        flash_stream_read(&flash_ota, NewImg2Addr+i, rlen, buf);
                        for(k = 0; k < rlen; k++)
                                flash_checksum+=buf[k];
                }

                ret = update_ota_checksum(file_checksum, flash_checksum, NewImg2Addr);
                if(ret == -1){
                        printf("\r\nThe checksume is wrong!\r\n");
                        printf("\nUpdate fail! Result send to Server\n");
                        goto update_ota_exit;
                }else{
                        printf("\nUpdate OK! Result send to Server\n");
                        array2[2] = 0x02;
                        array2[3] = (array2[1] + array2[2]) & 0xFF;
                        write(server_fd, array2, sizeof(array2)); //Send Protocol => Result
                        printf("Send Result OK!\n");
                }
        }
update_ota_exit:
        if(alloc)
                update_free(alloc);	
        if(server_fd >= 0)
                close(server_fd);
        printf("\n\r[%s] Update task exit", __FUNCTION__);	
        if(!ret){
                printf("\n\r[%s] Ready to reboot", __FUNCTION__);	
                ota_platform_reset();
        }
        vTaskDelete(NULL);	
        return ret;
}

void wlan_networkpaper()
{
        if(xTaskCreate(xinit_thread, ((const char*)"init"), STACKSIZE, NULL, tskIDLE_PRIORITY + 3, NULL) != pdPASS)
                printf("\n\r%s xTaskCreate(init_thread) failed", __FUNCTION__);

        if(xTaskCreate(socket_select_thread, ((const char*)"socket_select_thread"), STACKSIZE, NULL, tskIDLE_PRIORITY + 1, NULL) != pdPASS)
                printf("\n\r%s xTaskCreate(socket_select_thread) failed", __FUNCTION__);
}