#include "ext.h"
#include <cstdlib>       /* included for rand() function */

#define TSIZE 12          /* for 12x12 pitch class transition table size */

typedef struct {
   t_object max_data;      
   long     lastnote;     
   long     outnote;      
   long     ttable[TSIZE][TSIZE]; 
   void*    output;       
} MyObject;

void* object_data = NULL;

void   main                (void);
void*  create_object       (void);
void   InputTriggerNote    (MyObject* mo, long value);
void   InputTrainingNote   (MyObject* mo, long value);
void   InputBang           (MyObject* mo);
void   MessageClear        (MyObject* mo);
int    chooseNextNote      (int lastnote, long ttable[TSIZE][TSIZE]);
void   setAllValuesOfTable (long table[TSIZE][TSIZE], int value);
long   midilimit           (long value);
double randfloat           (void);

void main(void) {
   setup((t_messlist**)&object_data, (method)create_object,
         NULL, sizeof(MyObject), NULL, A_NOTHING);
   addbang((method)InputBang);
   addint ((method)InputTriggerNote);
   addinx ((method)InputTrainingNote, 1);
   addmess((method)MessageClear, "clear", A_NOTHING);
}

void* create_object(void) {
   MyObject* mo = (MyObject*)newobject(object_data);

   mo->lastnote   = 60;
   mo->outnote    = 60;
   setAllValuesOfTable(mo->ttable, 0);

   mo->output     = intout(mo);
   intin(mo, 1);
   return mo;
}

void InputTriggerNote(MyObject* mo, long value) {
   int octave;
   value       = midilimit(value);
   octave      = value / 12;
   mo->outnote = 12 * octave + chooseNextNote(mo->outnote, mo->ttable);

   outlet_int(mo->output, mo->outnote);
}

void InputTrainingNote(MyObject* mo, long value) {
   int newstate, laststate;
   value        = midilimit(value);
   laststate    = mo->lastnote % 12;
   newstate     = value % 12;
   mo->lastnote = value;

   mo->ttable[laststate][newstate]++;
}

void InputBang(MyObject* mo) {
   InputTriggerNote(mo, mo->outnote);
}

void MessageClear(MyObject* mo) {
   setAllValuesOfTable(mo->ttable, 0);
}


long midilimit(long value) {
   if (value < 0)     return   0;
   if (value > 127)   return 127;
   return value;
}

void setAllValuesOfTable(long table[TSIZE][TSIZE], int value) {
   int i, j;
   for (i=0; i<TSIZE; i++) {
      for (j=0; j<TSIZE; j++) {
         table[i][j] = value;
      }
   }
}

double randfloat(void) {
   return (double)rand()/RAND_MAX;
}

int chooseNextNote(int lastnote, long ttable[TSIZE][TSIZE]) {
   int targetSum   = 0;
   int sum         = 0;
   int nextnote    = 0;
   int totalevents = 0;
   int i;
   lastnote = lastnote % TSIZE;  
   for (i=0; i<TSIZE; i++) {
      totalevents += ttable[lastnote][i];
   }
   targetSum = (int)(randfloat() * totalevents + 0.5);
   while ((nextnote < TSIZE) && (sum+ttable[lastnote][nextnote] <= targetSum)) {
      sum += ttable[lastnote][nextnote];
      nextnote++;
   }
   return nextnote;
}
