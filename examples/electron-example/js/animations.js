function startTransitionAnimation() {
     $('.card').addClass('test');
     $('.card').addClass('testtwo');
     $(".authenticate").show().animate({
          right: -320
     }, {
          easing: 'easeOutQuint',
          duration: 600,
          queue: false
     });
     $(".authenticate").animate({
          opacity: 1
     }, {
          duration: 200,
          queue: false
     }).addClass('visible');
};

function cancelTransitionAnimation() {
     $('.card').removeClass('test');
     $('.card').removeClass('testtwo');
     $(".authenticate").hide().animate({
          right: 320
     }, {
          easing: 'easeOutQuint',
          duration: 600,
          queue: false
     });
     $(".authenticate").animate({
          opacity: 0
     }, {
          duration: 200,
          queue: false
     }).removeClass('visible');
}

function successAnimation() {
     $(".authenticate").show().animate({
          right: 320
     }, {
          easing: 'easeOutQuint',
          duration: 600,
          queue: false
     });
     $(".authenticate").animate({
          opacity: 0
     }, {
          duration: 200,
          queue: false
     }).removeClass('visible');

     $('.card').removeClass('testtwo');
     $('.card').removeClass('test');
     $('.card').removeClass('register');
     $('.card').removeClass('login');
     $('.card').addClass('success');

}

function logoutAnimation(){
     $('.card').addClass('test');
     setTimeout(function() {
          $('.card').removeClass('success');
          $('.card').addClass('login');
     }, 400);

     setTimeout(function() {
          $('.card').removeClass('test');
          $('*').stop();
     }, 800);
     cancelTransitionAnimation();
}

function stopAuthenticateAnimation() {
     $(".authenticate").show().animate({
          right: 90
     }, {
          easing: 'easeOutQuint',
          duration: 600,
          queue: false
     });
     $(".authenticate").animate({
          opacity: 0
     }, {
          duration: 200,
          queue: false
     }).addClass('visible');
     $('.card').removeClass('testtwo');
     $('.card').removeClass('test');
     $('.login').fadeOut(123);
     $('.login.success').fadeIn();
}

function registerToLogin() {
     $('.card').addClass('test');
     setTimeout(function() {
          $('.card').removeClass('register');
          $('.card').addClass('login');

     }, 400);

     setTimeout(function() {
          $('.card').removeClass('test');
     }, 800);

}

function loginToRegister() {
     $('.card').addClass('test');
     setTimeout(function() {
          $('.card').addClass('register');
          $('.card').removeClass('login');
     }, 400);

     setTimeout(function() {
          $('.card').removeClass('test');
     }, 800);
}

$('input[type="text"],input[type="password"]').focus(function() {
     $(this).prev().animate({
          'opacity': '1'
     }, 200)
});
$('input[type="text"],input[type="password"]').blur(function() {
     $(this).prev().animate({
          'opacity': '.5'
     }, 200)
});

$('input[type="text"],input[type="password"]').keyup(function() {
     if (!$(this).val() == '') {
          $(this).next().animate({
               'opacity': '1',
               'right': '30'
          }, 200)
     } else {
          $(this).next().animate({
               'opacity': '0',
               'right': '20'
          }, 200)
     }
});

$('.tab').click(function() {
     $(this).fadeOut(200, function() {
          $(this).parent().animate({
               'left': '0'
          })
     });
});

$('#phone-select').css('color', '#DC6180')
$('#phone-select').change(function() {
     var current = $('#phone-select').val();
     if (current != 'null' && current != 'device') {
          $('#phone-select').css('color', '#DC6180');
          $(this).next().animate({
               'opacity': '1',
               'right': '30'
          }, 200)
     } else {
          $('#phone-select').css('color', '#4E546D');
          $(this).next().animate({
               'opacity': '0',
               'right': '20'
          }, 200)
     }
});

function showPhoneTick() {
     $('#phone-select').css('color', '#DC6180');
     $('#phone-select').next().stop().animate({
          'opacity': '1',
          'right': '30'
     }, 200)
}

function hidePhoneTick() {
     $('#phone-select').css('color', '#4E546D');
     $('#phone-select').next().stop().animate({
          'opacity': '0',
          'right': '20'
     }, 200)
}

$("select").on({
     "change": function() {
          $(this).blur();
     },

     'focus': function() {
          $(this).prev().animate({
               'opacity': '1'
          }, 200)
     },

     "blur": function() {
          $(this).prev().animate({
               'opacity': '.5'
          }, 200)
     },

     "keyup": function(e) {
          if (e.keyCode == 27) {
               $(this).prev().animate({
                    'opacity': '1'
               }, 200)
          }
     }
});
