# twilio module to send and verify otp code by sms

## Send OTP code

Call the GET rest method `https://mydomain/meveo/rest/otp/:toNumber`

*Input*
 the path parameter :toNumber is the twilio formated (international form) to which the code will be sent.

*Response*
This method returns a simple "text/plain" response code.

this method first check if an OTP sms has already been sent to this number.

If there is one that has been sent in the last 30 seconds then the reponse code is `retry_later`

If there has been more than 5 OTP request in the day the response code is `too_many_requests`

If there is a pb with the twilio service or database connection the response code is `server_error
`

If OTP code has been sent correctly the response is `accepted`

In that case the code can been found in meveo/Messaging/OutboundSMS table.


## Verify OTP code


Call the GET rest method `https://mydomain/meveo/rest/verify_otp/:toNumber?code=:otpCode`

*Input*
 the path parameter :toNumber is the twilio formated (international form) to which the code has been sent.

 the query parameter code is the 6 digit code received by the user

*Response*
This method returns a simple "text/plain" response code.

this method first check if an OTP sms has already been sent to this number and not yet verified.

If there is no pending verification or one that has been sent more that 3 minutes ago or the user tried more than 5 codes the reponse is `invalid_request`

If the code is incorrect the response `invalid_code`

If there is a pb with the twilio service or database connection the response code is `server_error`

If OTP code is correct response is `success`
