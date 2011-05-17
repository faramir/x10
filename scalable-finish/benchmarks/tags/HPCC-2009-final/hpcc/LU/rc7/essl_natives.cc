
#ifdef __cplusplus
extern "C"
#endif
void dtrsm_(char *, char *, char *, char *, 
	   int *, int *, double *, void *, int *, void *, int *);

#ifdef __cplusplus
extern "C"
#endif
void dgemm_(char *, char *, int *, int *, int *, double *, void *,
	   int *, void *, int *, double *, void *, int *);



extern "C" {

void blockTriSolve (double* me, 
			      double* diag, 
			      signed int B)
{
  double alpha = 1.0;
  int one = 1;
  dtrsm_("R", "U", "N", "U", &B, &B, &alpha, diag, &B, me, &B);
  
}

void blockBackSolve (double* me, 
			       double* diag, 
			       signed int B)
{
  double alpha = 1.0;
  int one = 1;
  dtrsm_("R", "L", "N", "N", &one, &B, &alpha, diag, &B, me, &B);
}

void blockMulSub (double* me,
			    double* left, 
			    double* upper, 
			    signed int B)
{
  double alpha = -1.0;
  double beta = 1.0;
  dgemm_ ("N", "N", &B, &B, &B, &alpha, upper, &B, left, &B, &beta, me, &B);  
}


void blockMulSubRow (double     * me,
				       double     * diag,
				       signed int   B,
				       signed int   j,
				       bool cond)
{
  int i, k;

  if (cond)
    {
      double div0 = 1.0 / me[B*j+j];
      for (i = j+1; i < B; i++)
	{
	  me[i*B+j] *= div0;
	  for (k=j+1; k<B; k++) me[i*B+k] -= me[i*B+j]*me[j*B+k];
	}
    }
  else
    {
      double div0 = 1.0 / diag[j];
      for (i=0; i<B; i++)
	{
	  me[i*B+j] *= div0;
	  for (k=j+1; k<B; k++) me[i*B+k] -= me[i*B+j]*diag[k];
	}
    }
}

#if TRANSPORT == bgp
void _xliltrm() { }
void flush_ () { }
void _xlfStop () { }
void _xlf_create_threadlocal() {}
void _fill () { }
#endif
}