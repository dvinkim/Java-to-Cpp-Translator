/* Test for string translation.  */
// {{ dg-checkwhat "c-analyzer" }}
// {{ dg-preprocess "Need preprocessing" }}
int main()
{
  unsigned long int *ptr;
  ptr = ((unsigned long int *)
         ( { void *stack_ptr;
           __asm__ __volatile__ ( "foo %0" : "=r" (stack_ptr) );
           (stack_ptr); } ) );
  return 0;
}
