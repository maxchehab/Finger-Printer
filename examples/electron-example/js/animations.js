function startPairAnimation() {
     $('.card').addClass('test');
     $('.card').addClass('testtwo');
     $(".pair").show().animate({
          right: -320
     }, {
          easing: 'easeOutQuint',
          duration: 600,
          queue: false
     });
     $(".pair").animate({
          opacity: 1
     }, {
          duration: 200,
          queue: false
     }).addClass('visible');
};

function cancelPairAnimation() {
     $('.card').removeClass('test');
     $('.card').removeClass('testtwo');
     $(".pair").hide().animate({
          right: 320
     }, {
          easing: 'easeOutQuint',
          duration: 600,
          queue: false
     });
     $(".pair").animate({
          opacity: 0
     }, {
          duration: 200,
          queue: false
     }).removeClass('visible');
}

function stopPairAnimation() {
     $(".pair").show().animate({
          right: 90
     }, {
          easing: 'easeOutQuint',
          duration: 600,
          queue: false
     });
     $(".pair").animate({
          opacity: 0
     }, {
          duration: 200,
          queue: false
     }).addClass('visible');
     $('.card').removeClass('testtwo');
     $('.card').removeClass('test');
     $('.register').fadeOut(123);
     $('.register.success').fadeIn();
}

function registerToLogin() {
     $('.card').addClass('test');
     setTimeout(function() {
          $('.register').fadeOut(0);
          $('.login').fadeIn(123);
     }, 400);

     setTimeout(function() {
          $('.card').removeClass('test');
     }, 800);

}

function loginToRegister() {
     $('.card').addClass('test');
     setTimeout(function() {
          $('.login').fadeOut(0);
          $('.register').fadeIn(123);
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
     if (current != 'null') {
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
     $('#phone-select').next().animate({
          'opacity': '1',
          'right': '30'
     }, 200)
}

function hidePhoneTick() {
     $('#phone-select').css('color', '#4E546D');
     $('#phone-select').next().animate({
          'opacity': '0',
          'right': '20'
     }, 200)
}

$("#phone-select").on({
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
